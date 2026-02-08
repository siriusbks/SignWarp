package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.gui.WarpGui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SWCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;

    public SWCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /signwarp <gui|reload|confirmwarpdelete>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                    return true;
                }
                Player player = (Player) sender;
                if (!player.hasPermission("signwarp.admin")) {
                    player.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.not_permission",
                            "You don't have permission to use this command."));
                    return true;
                }
                WarpGui.openWarpGui(player, 0);
                return true;

            case "reload":
                if (!sender.hasPermission("signwarp.reload")) {
                    sender.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.not_permission",
                            "You don't have permission to use this command."));
                    return true;
                }
                plugin.reloadConfig();
                EventListener.updateConfig(plugin);
                sender.sendMessage(ChatColor.GREEN + "Configuration successfully reloaded.");
                return true;

            case "confirmwarpdelete":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                    return true;
                }
                Player playerToDelete = (Player) sender;
                org.bukkit.block.Block block = EventListener.getPendingDeletion(playerToDelete.getUniqueId());

                if (block == null) {
                    playerToDelete.sendMessage(ChatColor.RED + "You have no pending warp deletion.");
                    return true;
                }

                org.bukkit.block.Sign sign = fr.nbstudio.signwarp.utils.SignUtils.getSignFromBlock(block);

                if (sign == null) {
                    playerToDelete.sendMessage(ChatColor.RED + "The sign is no longer valid.");
                    EventListener.removePendingDeletion(playerToDelete.getUniqueId());
                    return true;
                }

                SignData signData = new SignData(sign.getSide(org.bukkit.block.sign.Side.FRONT).getLines());
                Warp warp = Warp.getByName(signData.warpName);

                if (warp != null) {
                    warp.remove();
                    playerToDelete.sendMessage(ChatColor.GREEN + "Warp '" + warp.getName() + "' deleted successfully.");
                } else {
                    playerToDelete.sendMessage(ChatColor.RED + "Warp not found in database.");
                }

                block.setType(org.bukkit.Material.AIR);
                EventListener.removePendingDeletion(playerToDelete.getUniqueId());
                return true;

            default:
                sender.sendMessage(
                        ChatColor.RED + "Unknown subcommand. Usage: /signwarp <gui|reload|confirmwarpdelete>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("signwarp.admin") && "gui".startsWith(args[0].toLowerCase())) {
                completions.add("gui");
            }
            if (sender.hasPermission("signwarp.reload") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if (sender.hasPermission("signwarp.break") && "confirmwarpdelete".startsWith(args[0].toLowerCase())) {
                completions.add("confirmwarpdelete");
            }
        }
        return completions;
    }
}
