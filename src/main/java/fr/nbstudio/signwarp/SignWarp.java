package fr.nbstudio.signwarp;

import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignWarp extends JavaPlugin {

    public void onEnable() {
        saveDefaultConfig();
        SWReloadCommand reloadCommand = new SWReloadCommand(this);
        PluginCommand command = getCommand("signwarp");
        if (command != null) {
            command.setExecutor(reloadCommand);
            command.setTabCompleter((TabCompleter) reloadCommand);
        } else {
            getLogger().warning("Command 'signwarp' not found!");
        }

        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new EventListener(this), this);

    }

}
