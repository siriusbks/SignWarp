package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.bstats.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignWarp extends JavaPlugin {

    private static final int RESOURCE_ID = 116195;
    private static final String PLUGIN_URL = "https://www.spigotmc.org/resources/signwarp-teleport-using-the-signs." + RESOURCE_ID + "/";

    public void onEnable() {
        // Check for updates
        new UpdateChecker(this, RESOURCE_ID).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("No new version available.");
            } else {
                getLogger().info("A new version of the plugin is available: " + version + " (current: " + this.getDescription().getVersion() + "). Download it here: " + PLUGIN_URL);
            }
        });

        // Save default config
        saveDefaultConfig();

        // Initialize bStats
        int pluginId = 21626;
        new Metrics(this, pluginId);

        // Initialize database
        Warp.createTable();

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