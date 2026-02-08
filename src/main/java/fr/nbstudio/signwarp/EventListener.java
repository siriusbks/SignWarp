package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.utils.SignUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;

public class EventListener implements Listener {
    private final SignWarp plugin;
    private static FileConfiguration config;
    private final HashMap<UUID, BukkitTask> teleportTasks = new HashMap<>();
    private final HashSet<UUID> invinciblePlayers = new HashSet<>();
    private final HashMap<UUID, Double> pendingTeleportCosts = new HashMap<>();
    private final HashMap<UUID, Integer> pendingItemCosts = new HashMap<>();
    private static final HashMap<UUID, Block> pendingDeletions = new HashMap<>();

    EventListener(SignWarp plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    // method static to update the config
    public static void updateConfig(JavaPlugin plugin) {
        config = plugin.getConfig();
    }

    public static Block getPendingDeletion(UUID playerUUID) {
        return pendingDeletions.get(playerUUID);
    }

    public static void removePendingDeletion(UUID playerUUID) {
        pendingDeletions.remove(playerUUID);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) throws IOException {
        SignData signData = new SignData(event.getLines());

        if (!signData.isWarpSign()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("signwarp.create")) {
            String noPermissionMessage = config.getString("messages.error.create-permission");
            if (noPermissionMessage != null) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            event.setCancelled(true);
            return;
        }

        if (!signData.isValidWarpName()) {
            String noWarpNameMessage = config.getString("messages.error.no-warp-name");
            if (noWarpNameMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noWarpNameMessage));
            }
            event.setCancelled(true);
            return;
        }

        if (signData.isWarpTarget()) {
            int warpLimit = getWarpLimit(player);
            if (warpLimit != -1) {
                long currentWarps = Warp.getAll().size();

                if (currentWarps >= warpLimit) {
                    String limitMessage = config.getString("messages.error.limit-reached",
                            "&cYou have reached your warp creation limit ({limit}).");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            limitMessage.replace("{limit}", String.valueOf(warpLimit))));
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Warp existingWarp = Warp.getByName(signData.warpName);

        if (signData.isWarp()) {
            if (existingWarp == null) {
                String warpNotFoundMessage = config.getString("messages.error.warp-not-found");
                if (warpNotFoundMessage != null) {
                    player.sendMessage(
                            ChatColor.translateAlternateColorCodes('&', warpNotFoundMessage));
                }
                event.setCancelled(true);
                return;
            }

            event.setLine(0, ChatColor.BLUE + SignData.HEADER_WARP);

            // Track the warp sign location for bi-directional teleportation
            WarpSignLink warpLink = new WarpSignLink(signData.warpName, event.getBlock().getLocation(), "WARP");
            warpLink.save();

            String warpCreatedMessage = config.getString("messages.success.warp-created");
            if (warpCreatedMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', warpCreatedMessage));
            }
        } else {
            if (existingWarp != null) {
                String warpNameTakenMessage = config.getString("messages.error.warp-exists");
                if (warpNameTakenMessage != null) {
                    player.sendMessage(
                            ChatColor.translateAlternateColorCodes('&', warpNameTakenMessage));
                }
                event.setCancelled(true);
                return;
            }

            String currentDateTime = java.time.LocalDateTime.now().toString();
            Warp warp = new Warp(signData.warpName, player.getLocation(), currentDateTime);
            warp.save();

            event.setLine(0, ChatColor.BLUE + SignData.HEADER_TARGET);

            // Track the target sign location for bi-directional teleportation
            WarpSignLink targetLink = new WarpSignLink(signData.warpName, event.getBlock().getLocation(), "TARGET");
            targetLink.save();

            String targetSignCreatedMessage = config.getString("messages.success.target-created");
            if (targetSignCreatedMessage != null) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', targetSignCreatedMessage));
            }
        }
    }

    private int getWarpLimit(Player player) {
        if (player.hasPermission("signwarp.limit.unlimited"))
            return -1;

        int maxLimit = -1;
        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission();
            if (perm.startsWith("signwarp.limit.")) {
                try {
                    int limit = Integer.parseInt(perm.substring("signwarp.limit.".length()));
                    if (limit > maxLimit)
                        maxLimit = limit;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return maxLimit;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) throws IOException {
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!Tag.ALL_SIGNS.isTagged(blockType)) {
            if (hasBlockWarpSign(block)) {
                event.setCancelled(true);
            }
            return;
        }

        Sign signBlock = SignUtils.getSignFromBlock(block);

        if (signBlock == null) {
            return;
        }

        SignData signData = new SignData(signBlock.getSide(Side.FRONT).getLines());

        if (!signData.isWarpSign()) {
            return;
        }

        if (!signData.isValidWarpName()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("signwarp.create")) {
            String noPermissionMessage = config.getString("messages.error.destroy-permission");
            if (noPermissionMessage != null) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("signwarp.break")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to break Warp signs.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (signData.isWarpTarget()) {
            Warp warp = Warp.getByName(signData.warpName);
            if (warp != null) {
                warp.remove();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.success.warp-deleted")));
            }
        } else {
            // For warp signs, just notify that the sign was removed
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.success.warp-removed", "&aWarp sign removed successfully!")));
        }
        if (pendingDeletions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW
                    + "You already have a pending Warp sign deletion. Type /signwarp confirmwarpdelete to confirm.");
            return;
        }

        pendingDeletions.put(player.getUniqueId(), block);
        player.sendMessage(ChatColor.YELLOW
                + "Are you sure you want to delete this Warp sign? Type /signwarp confirmwarpdelete within 10 seconds to confirm.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingDeletions.containsKey(player.getUniqueId())
                        && pendingDeletions.get(player.getUniqueId()).equals(block)) {
                    pendingDeletions.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.RED + "Warp deletion request expired.");
                }
            }
        }.runTaskLater(plugin, 200);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        Sign signBlock = SignUtils.getSignFromBlock(block);

        if (signBlock == null) {
            return;
        }

        SignData signData = new SignData(signBlock.getSide(Side.FRONT).getLines());

        if (signData.isWarpSign() && !signBlock.isWaxed()) {
            signBlock.setWaxed(true);
            signBlock.update();
        }

        // Check if it's a valid warp sign (either [Warp] or [WarpTarget])
        if (!signData.isWarpSign() || !signData.isValidWarpName()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("signwarp.use")) {
            String noPermissionMessage = config.getString("messages.error.use-permission");
            if (noPermissionMessage != null) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            return;
        }

        double teleportCost = config.getDouble("costs.money", 0.0);
        String useItem = config.getString("costs.item.material", "NONE");
        int useCost = config.getInt("costs.item.amount", 0);

        if ("none".equalsIgnoreCase(useItem)) {
            useItem = null;
        }

        if (teleportCost > 0 && useItem == null) {
            // Case where no item is required and a teleportation cost is applied
            Economy economy = VaultEconomy.getEconomy();
            if (economy == null) {
                player.sendMessage(ChatColor.RED
                        + "Vault is required for teleportation cost, but it is not installed or enabled.");
                return;
            }

            if (economy.getBalance(player) < teleportCost) {
                String noMoney = config.getString("messages.error.no-money", "&cNot enough money.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        noMoney.replace("{cost}", String.valueOf(teleportCost))));
                return;
            }

            // Temporarily store the teleport cost to be deducted after teleportation
            pendingTeleportCosts.put(player.getUniqueId(), teleportCost);

            // For bi-directional teleportation
            if (signData.isWarpTarget()) {
                // Teleport from target back to warp sign
                teleportPlayerBidirectional(player, signData.warpName, "TARGET", true, teleportCost);
            } else {
                // Original teleport from warp to target
                teleportPlayer(player, signData.warpName, true, teleportCost);
            }
        } else if (teleportCost == 0.0 && useItem != null) {
            // Case where a specific item is required and the teleportation cost is 0
            Material itemInHand = event.getMaterial();

            if (itemInHand != null && itemInHand.name().equalsIgnoreCase(useItem)) {
                if (useCost > event.getItem().getAmount()) {
                    String notEnoughItemMessage = config.getString("messages.error.no-item");
                    if (notEnoughItemMessage != null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                notEnoughItemMessage.replace("{amount}", String.valueOf(useCost))
                                        .replace("{item}", useItem)));
                    }
                    return;
                }

                pendingItemCosts.put(player.getUniqueId(), useCost);

                // For bi-directional teleportation
                if (signData.isWarpTarget()) {
                    // Teleport from target back to warp sign
                    teleportPlayerBidirectional(player, signData.warpName, "TARGET", false, 0);
                } else {
                    // Original teleport from warp to target
                    teleportPlayer(player, signData.warpName, false, 0);
                }
            } else {
                String invalidItemMessage = config.getString("messages.error.invalid-item");
                if (invalidItemMessage != null) {
                    player.sendMessage(
                            ChatColor.translateAlternateColorCodes('&', invalidItemMessage
                                    .replace("{item}", useItem != null ? useItem : "an item")));
                }
            }
        } else if (teleportCost == 0.0 && useItem == null) {
            // Case where neither an item nor a teleportation cost is required

            // For bi-directional teleportation
            if (signData.isWarpTarget()) {
                // Teleport from target back to warp sign
                teleportPlayerBidirectional(player, signData.warpName, "TARGET", false, 0);
            } else {
                // Original teleport from warp to target
                teleportPlayer(player, signData.warpName, false, 0);
            }
        } else {
            // Case where both item and money can be used for teleportation
            player.sendMessage(ChatColor.RED + "You must use an item or pay to teleport.");
        }
    }

    private void teleportPlayer(Player player, String warpName, boolean useEconomy, double cost) {
        Warp warp = Warp.getByName(warpName);

        if (warp == null) {
            String warpNotFoundMessage = config.getString("messages.error.warp-not-found");
            if (warpNotFoundMessage != null) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', warpNotFoundMessage));
            }
            return;
        }

        int cooldown = config.getInt("teleport.cooldown", 5);

        String teleportMessage = config.getString("messages.success.teleport-start");
        if (teleportMessage != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    teleportMessage.replace("{warp-name}", warp.getName()).replace("{time}",
                            String.valueOf(cooldown))));
        }
        // Cancel any previous teleport tasks for the player
        UUID playerUUID = player.getUniqueId();
        BukkitTask previousTask = teleportTasks.get(playerUUID);
        if (previousTask != null) {
            previousTask.cancel();
        }

        // Add the player to the invincible list
        invinciblePlayers.add(playerUUID);

        // Schedule the new teleport task
        BukkitTask teleportTask = new BukkitRunnable() {
            int timeLeft = cooldown;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    teleportTasks.remove(playerUUID);
                    invinciblePlayers.remove(playerUUID);
                    return;
                }

                if (timeLeft > 0) {
                    if (config.getBoolean("teleport.action-bar", true)) {
                        String msg = ChatColor.GREEN + "Teleportation in " + ChatColor.YELLOW + timeLeft
                                + ChatColor.GREEN
                                + "s...";
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(msg));
                    }

                    // Play Sound
                    String warmupSoundName = config.getString("teleport.sound.warmup", "BLOCK_NOTE_BLOCK_PLING");
                    try {
                        Sound warmupSound = Sound.valueOf(warmupSoundName);
                        player.playSound(player.getLocation(), warmupSound, 1f, 2f);
                    } catch (IllegalArgumentException ignored) {
                    }

                    // Spawn Particles
                    String particleName = config.getString("teleport.particle", "PORTAL");
                    try {
                        Particle particle = Particle.valueOf(particleName);
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5);
                    } catch (IllegalArgumentException ignored) {
                    }

                    timeLeft--;
                } else {
                    // Teleport Logic
                    Location targetLocation = warp.getLocation();
                    player.teleport(targetLocation);

                    String soundName = config.getString("teleport.sound.success", "ENTITY_ENDERMAN_TELEPORT");

                    // Logic to use particle from config if needed, or stick to Particle logic
                    // above.
                    // Old 'teleport-effect' is removed.
                    // We already use spawnParticle in the loop. Maybe on success too?
                    // player.getWorld().spawnParticle(...)

                    Sound sound;
                    try {
                        sound = Sound.valueOf(soundName);
                    } catch (IllegalArgumentException e) {
                        sound = Sound.ENTITY_ENDERMAN_TELEPORT; // fallback to default
                    }

                    World world = targetLocation.getWorld();
                    world.playSound(targetLocation, sound, 1, 1);

                    if (config.getBoolean("teleport.action-bar", true)) {
                        // Clear Action Bar
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(""));
                    }

                    String successMessage = config.getString("messages.success.teleport-done");
                    if (successMessage != null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                successMessage.replace("{warp}", warp.getName())));
                    }

                    // Deduct cost after successful teleportation
                    if (useEconomy) {
                        Double teleportCost = pendingTeleportCosts.remove(playerUUID);
                        if (teleportCost != null) {
                            Economy economy = VaultEconomy.getEconomy();
                            economy.withdrawPlayer(player, teleportCost);

                            // Notify the player of the cost
                            String notifyCostMessage = config.getString("messages.success.transaction");
                            if (notifyCostMessage != null) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        notifyCostMessage.replace("{cost}", String.valueOf(teleportCost))));
                            }
                        }
                    } else {
                        Integer itemCost = pendingItemCosts.remove(playerUUID);
                        if (itemCost != null && itemCost > 0) {
                            Material itemInHand = player.getInventory().getItemInMainHand().getType();
                            player.getInventory().removeItem(new ItemStack(itemInHand, itemCost));
                        }
                    }

                    // Cleanup
                    teleportTasks.remove(playerUUID);
                    invinciblePlayers.remove(playerUUID);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        // Store the task in the map
        teleportTasks.put(playerUUID, teleportTask);
    }

    private void teleportPlayerBidirectional(Player player, String warpName, String currentSignType, boolean useEconomy,
            double cost) {
        // Find the opposite end location
        Location targetLocation = WarpSignLink.getOtherEndLocation(warpName, currentSignType);

        if (targetLocation == null) {
            String noLinkMessage = config.getString("messages.error.no-link",
                    "&cNo return point found for this warp!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noLinkMessage));
            return;
        }

        int cooldown = config.getInt("teleport.cooldown", 5);

        String teleportMessage = config.getString("messages.success.teleport-start");
        if (teleportMessage != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    teleportMessage.replace("{warp-name}", warpName).replace("{time}",
                            String.valueOf(cooldown))));
        }

        UUID playerUUID = player.getUniqueId();

        // Cancel any previous teleport tasks for the player
        BukkitTask previousTask = teleportTasks.get(playerUUID);
        if (previousTask != null) {
            previousTask.cancel();
        }

        // Add the player to the invincible list
        invinciblePlayers.add(playerUUID);

        // Schedule the new teleport task
        BukkitTask teleportTask = new BukkitRunnable() {
            int timeLeft = cooldown;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    teleportTasks.remove(playerUUID);
                    invinciblePlayers.remove(playerUUID);
                    return;
                }

                if (timeLeft > 0) {
                    if (config.getBoolean("teleport.action-bar", true)) {
                        String msg = ChatColor.GREEN + "Teleportation in " + ChatColor.YELLOW + timeLeft
                                + ChatColor.GREEN
                                + "s...";
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(msg));
                    }

                    // Play Sound
                    String warmupSoundName = config.getString("teleport.sound.warmup", "BLOCK_NOTE_BLOCK_PLING");
                    try {
                        Sound warmupSound = Sound.valueOf(warmupSoundName);
                        player.playSound(player.getLocation(), warmupSound, 1f, 2f);
                    } catch (IllegalArgumentException ignored) {
                    }

                    // Spawn Particles
                    String particleName = config.getString("teleport.particle", "PORTAL");
                    try {
                        Particle particle = Particle.valueOf(particleName);
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5);
                    } catch (IllegalArgumentException ignored) {
                    }

                    timeLeft--;
                } else {
                    player.teleport(targetLocation);

                    String soundName = config.getString("teleport.sound.success", "ENTITY_ENDERMAN_TELEPORT");

                    Sound sound;
                    try {
                        sound = Sound.valueOf(soundName);
                    } catch (IllegalArgumentException e) {
                        sound = Sound.ENTITY_ENDERMAN_TELEPORT; // fallback to default
                    }

                    World world = targetLocation.getWorld();
                    world.playSound(targetLocation, sound, 1, 1);

                    if (config.getBoolean("teleport.action-bar", true)) {
                        // Clear Action Bar
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(""));
                    }

                    String successMessage = config.getString("messages.success.teleport-done");
                    if (successMessage != null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                successMessage.replace("{warp}", warpName)));
                    }

                    // Deduct cost after successful teleportation
                    if (useEconomy) {
                        Double teleportCost = pendingTeleportCosts.remove(playerUUID);
                        if (teleportCost != null) {
                            Economy economy = VaultEconomy.getEconomy();
                            economy.withdrawPlayer(player, teleportCost);

                            // Notify the player of the cost
                            String notifyCostMessage = config.getString("messages.success.transaction");
                            if (notifyCostMessage != null) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        notifyCostMessage.replace("{cost}", String.valueOf(teleportCost))));
                            }
                        }
                    } else {
                        Integer itemCost = pendingItemCosts.remove(playerUUID);
                        if (itemCost != null && itemCost > 0) {
                            Material itemInHand = player.getInventory().getItemInMainHand().getType();
                            player.getInventory().removeItem(new ItemStack(itemInHand, itemCost));
                        }
                    }

                    // Cleanup
                    teleportTasks.remove(playerUUID);
                    invinciblePlayers.remove(playerUUID);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Store the task in the map
        teleportTasks.put(playerUUID, teleportTask);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (teleportTasks.containsKey(playerUUID)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                BukkitTask teleportTask = teleportTasks.get(playerUUID);
                if (teleportTask != null && !teleportTask.isCancelled()) {
                    teleportTask.cancel();
                    teleportTasks.remove(playerUUID);
                    invinciblePlayers.remove(playerUUID); // Remove invincibility
                    pendingTeleportCosts.remove(playerUUID); // Remove pending teleport cost
                    pendingItemCosts.remove(playerUUID); // Remove pending item cost
                    String cancelMessage = config.getString("messages.success.teleport-cancelled",
                            "&cTeleportation cancelled.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', cancelMessage));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerUUID = player.getUniqueId();
            if (teleportTasks.containsKey(playerUUID)) {
                // Cancel teleport instead of cancelling damage
                BukkitTask task = teleportTasks.remove(playerUUID);
                if (task != null) {
                    task.cancel();
                }
                invinciblePlayers.remove(playerUUID);
                pendingTeleportCosts.remove(playerUUID);
                pendingItemCosts.remove(playerUUID);

                String cancelMessage = config.getString("messages.success.teleport-cancelled",
                        "&cTeleportation cancelled due to damage.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', cancelMessage));
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (hasBlockWarpSign(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (hasBlockWarpSign(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (hasBlockWarpSign(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (hasBlockWarpSign(event.blockList())) {
            event.setCancelled(true);
        }
    }

    private boolean hasBlockWarpSign(Block block) {
        return SignUtils.hasBlockSign(block, this::isWarpSign);
    }

    private boolean hasBlockWarpSign(List<Block> blocks) {
        return SignUtils.hasBlockSign(blocks, this::isWarpSign);
    }

    private boolean isWarpSign(Sign signBlock) {
        SignData signData = new SignData(signBlock.getSide(Side.FRONT).getLines());
        return signData.isWarpSign();
    }
}