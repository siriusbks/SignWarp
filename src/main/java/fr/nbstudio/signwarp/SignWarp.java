package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.bstats.Metrics;
import fr.nbstudio.signwarp.gui.WarpGuiListener;
import org.bukkit.ChatColor;
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
                getLogger().info("No new version available");
            } else {
                getLogger().warning("A new version of the plugin is available: " + version + " (current: " + this.getDescription().getVersion() + "). Download it here: " + PLUGIN_URL);
            }
        });

        // Setup Vault economy if available
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            if (!VaultEconomy.setupEconomy()) {
                getLogger().warning("Vault is installed but economy setup failed.");
            } else {
                getLogger().info("Vault economy setup successfully.");
            }
        } else {
            getLogger().warning("Vault not found. Economy features are disabled.");
        }

        // Save default config
        saveDefaultConfig();

        // Initialize bStats
        int pluginId = 21626;
        new Metrics(this, pluginId);

        // Initialize database and migrate table if needed
        Warp.createTable();

        // Register commands and tab completer
        PluginCommand command = getCommand("signwarp");
        if (command != null) {
            SWCommand swCommand = new SWCommand(this);
            command.setExecutor(swCommand);
            command.setTabCompleter(swCommand);
        } else {
            getLogger().warning("Command 'signwarp' not found!");
        }

        // Register event listener
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new EventListener(this), this);
        pluginManager.registerEvents(new WarpGuiListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}