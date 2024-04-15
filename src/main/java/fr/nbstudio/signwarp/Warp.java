package fr.nbstudio.signwarp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Warp {
    private final FileConfiguration fileConfiguration;
    private final JavaPlugin plugin;
    private String warpName;
    private Location location;

    Warp(FileConfiguration fileConfiguration, JavaPlugin plugin, String warpName, Location location) {
        this.fileConfiguration = fileConfiguration;
        this.plugin = plugin;
        this.warpName = warpName;
        this.location = location;
    }

    private static String getConfigPath(String warpName) {
        return "warps." + warpName;
    }

    static Warp getByName(FileConfiguration fileConfiguration, JavaPlugin plugin, String warpName) {
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(getConfigPath(warpName));

        if (configurationSection == null) {
            return null;
        }

        World world = plugin.getServer().getWorld(configurationSection.getString("world"));
        double posX = configurationSection.getDouble("x");
        double posY = configurationSection.getDouble("y");
        double posZ = configurationSection.getDouble("z");
        double yaw = configurationSection.getDouble("yaw");
        double pitch = configurationSection.getDouble("pitch");

        Location location = new Location(world, posX, posY, posZ, (float) yaw, (float) pitch);

        return new Warp(fileConfiguration, plugin, warpName, location);
    }

    static List<Warp> getAll(FileConfiguration fileConfiguration, JavaPlugin plugin) {
        ConfigurationSection warpsConfig = fileConfiguration.getConfigurationSection("warps");

        List<Warp> warps = new ArrayList<>();

        if (warpsConfig != null) {
            for (String warpName : warpsConfig.getKeys(false)) {
                warps.add(getByName(fileConfiguration, plugin, warpName));
            }
        }

        return warps;
    }

    void save() throws IOException {
        String configPath = getConfigPath(warpName);
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(configPath);

        if (configurationSection == null) {
            configurationSection = fileConfiguration.createSection(configPath);
        }

        configurationSection.set("world", location.getWorld().getName());
        configurationSection.set("x", location.getX());
        configurationSection.set("y", location.getY());
        configurationSection.set("z", location.getZ());
        configurationSection.set("yaw", location.getYaw());
        configurationSection.set("pitch", location.getPitch());

        fileConfiguration.save(plugin.getDataFolder() + File.separator + "config.yml");
    }

    void remove() throws IOException {
        fileConfiguration.set(getConfigPath(warpName), null);

        fileConfiguration.save(plugin.getDataFolder() + File.separator + "config.yml");
    }

    Location getLocation() {
        return location;
    }

    String getName() {
        return warpName;
    }
}
