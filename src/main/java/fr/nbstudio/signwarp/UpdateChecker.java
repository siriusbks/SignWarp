package fr.nbstudio.signwarp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getAsyncScheduler().runNow(plugin, (ScheduledTask task) -> {
            String latest = null;
            try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 Scanner scann = new Scanner(is)) {
                if (scann.hasNext()) {
                    latest = scann.next();
                }
            } catch (IOException e) {
                plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
            }

            final String result = latest;
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                if (result != null) {
                    consumer.accept(result);
                }
            });
        });
    }
}