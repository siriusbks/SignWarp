package fr.nbstudio.signwarp;

import fr.nbstudio.signwarp.bstats.Metrics;
import fr.nbstudio.signwarp.gui.WarpGuiListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignWarp extends JavaPlugin implements Listener {

    private static final int RESOURCE_ID = 116195;
    private static final String PLUGIN_URL = "https://www.spigotmc.org/resources/signwarp-teleport-using-the-signs." + RESOURCE_ID + "/";

    public void onEnable() {
        // Update check
        final String currentVersion = getDescription().getVersion();
        new UpdateChecker(this, RESOURCE_ID).getVersion(latest -> {
            if (currentVersion.equals(latest)) {
                getLogger().info("No new version available");
            } else {
                getLogger().warning("A new version of the plugin is available: " + latest +
                        " (current: " + currentVersion + "). Download it here: " + PLUGIN_URL);
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
        WarpSignLink.createTable();

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
        pluginManager.registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}