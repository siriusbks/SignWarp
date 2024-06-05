package fr.nbstudio.signwarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Warp {
    private static final String DB_URL = "jdbc:sqlite:" + JavaPlugin.getPlugin(SignWarp.class).getDataFolder() + File.separator + "warps.db";
    private final String warpName;
    private final Location location;

    public Warp(String warpName, Location location) {
        this.warpName = warpName;
        this.location = location;
    }

    public String getName() {
        return warpName;
    }

    public Location getLocation() {
        return location;
    }

    public void save() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                pstmt.setString(2, location.getWorld().getName());
                pstmt.setDouble(3, location.getX());
                pstmt.setDouble(4, location.getY());
                pstmt.setDouble(5, location.getZ());
                pstmt.setFloat(6, location.getYaw());
                pstmt.setFloat(7, location.getPitch());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void remove() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "DELETE FROM warps WHERE name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Warp getByName(String warpName) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM warps WHERE name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    Location location = new Location(world, x, y, z, yaw, pitch);
                    return new Warp(warpName, location);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Warp> getAll() {
        List<Warp> warps = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM warps";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    Location location = new Location(world, x, y, z, yaw, pitch);
                    warps.add(new Warp(name, location));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return warps;
    }

    public static void createTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "CREATE TABLE IF NOT EXISTS warps (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x REAL, " +
                    "y REAL, " +
                    "z REAL, " +
                    "yaw REAL, " +
                    "pitch REAL)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
