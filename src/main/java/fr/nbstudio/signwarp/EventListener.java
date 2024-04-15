package fr.nbstudio.signwarp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.io.IOException;
import java.util.List;

public class EventListener implements Listener {
    private final SignWarp plugin;
    private final FileConfiguration config;

    EventListener(SignWarp plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

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
            String noPermissionMessage = config.getString("messages.create_permission");
            if (noPermissionMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            event.setCancelled(true);
            return;
        }

        if (!signData.isValidWarpName()) {
            String noWarpNameMessage = config.getString("messages.no_warp_name");
            if (noWarpNameMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noWarpNameMessage));
            }
            event.setCancelled(true);
            return;
        }

        Warp existingWarp = Warp.getByName(plugin.getConfig(), plugin, signData.warpName);

        if (signData.isWarp()) {
            if (existingWarp == null) {
                String warpNotFoundMessage = config.getString("messages.warp_not_found");
                if (warpNotFoundMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', warpNotFoundMessage));
                }
                event.setCancelled(true);
                return;
            }

            event.setLine(0, ChatColor.BLUE + SignData.HEADER_WARP);

            String warpCreatedMessage = config.getString("messages.warp_created");
            if (warpCreatedMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', warpCreatedMessage));
            }
        } else {
            if (existingWarp != null) {
                String warpNameTakenMessage = config.getString("messages.warp_name_taken");
                if (warpNameTakenMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', warpNameTakenMessage));
                }
                event.setCancelled(true);
                return;
            }

            Warp warp = new Warp(plugin.getConfig(), plugin, signData.warpName, player.getLocation());
            warp.save();

            event.setLine(0, ChatColor.BLUE + SignData.HEADER_TARGET);

            String targetSignCreatedMessage = config.getString("messages.target_sign_created");
            if (targetSignCreatedMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', targetSignCreatedMessage));
            }
        }
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

        if (!signData.isWarpTarget()) {
            return;
        }

        if (!signData.isValidWarpName()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("signwarp.create")) {
            String noPermissionMessage = config.getString("messages.destroy_permission");
            if (noPermissionMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            event.setCancelled(true);
            return;
        }

        Warp warp = Warp.getByName(plugin.getConfig(), plugin, signData.warpName);

        if (warp == null) {
            return;
        }

        warp.remove();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.warp_destroyed")));
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

        if (!signData.isWarp() || !signData.isValidWarpName()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("signwarp.use")) {
            String noPermissionMessage = config.getString("messages.warp_use_permission");
            if (noPermissionMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
            }
            return;
        }

        FileConfiguration config = plugin.getConfig();

        String useItem = config.getString("use-item", "none");
        int useCost = config.getInt("use-cost", 0);

        // "none" should be equally to null
        if (useItem != null && useItem.equalsIgnoreCase("none")) {
            useItem = null;
        }

        Material itemInHand = event.getMaterial();

        if (useItem != null) {
            String invalidItemMessage = config.getString("messages.invalid_item");
            if (!itemInHand.name().equalsIgnoreCase(useItem)) {
                if (invalidItemMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidItemMessage.replace("{use-item}", useItem)));
                }
                return;
            }

            String notEnoughItemMessage = config.getString("messages.not_enough_item");
            if (useCost > event.getItem().getAmount()) {
                if (notEnoughItemMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', notEnoughItemMessage.replace("{use-cost}", String.valueOf(useCost)).replace("{use-item}", useItem)));
                }
                return;
            }
        }

        Warp warp = Warp.getByName(config, plugin, signData.warpName);

        if (warp == null) {
            String warpNotFoundMessage = config.getString("messages.warp_not_found");
            if (warpNotFoundMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', warpNotFoundMessage));
            }
            return;
        }

        if (useItem != null && useCost > 0) {
            event.getItem().setAmount(event.getItem().getAmount() - useCost);
        }

        Location targetLocation = warp.getLocation();

        player.teleport(targetLocation);

        World world = targetLocation.getWorld();
        world.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        world.playEffect(targetLocation, Effect.ENDER_SIGNAL, 10);

        String teleportMessage = config.getString("messages.teleport");
        if (teleportMessage != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', teleportMessage.replace("{warp-name}", warp.getName())));
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
