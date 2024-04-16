package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.bstats.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignWarp extends JavaPlugin {

    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize bStats
        int pluginId = 21626;
        new Metrics(this, pluginId);

        // Register commands and tab completer
        SWReloadCommand reloadCommand = new SWReloadCommand(this);
        PluginCommand command = getCommand("signwarp");
        if (command != null) {
            command.setExecutor(reloadCommand);
        } else {
            getLogger().warning("Command 'signwarp' not found!");
        }

        // Register event listener
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new EventListener(this), this);

    }

}
