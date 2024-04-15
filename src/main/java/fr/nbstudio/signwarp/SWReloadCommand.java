package fr.nbstudio.signwarp;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SWReloadCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public SWReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("signwarp.reload")) {
                String notPermissionMessage = config.getString("messages.not_permission");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', notPermissionMessage));
                return true;
            }

            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "SW - Reload complete!");
            return true;
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && "reload".startsWith(args[0])) {
            completions.add("reload");
        }
        return completions;
    }
}