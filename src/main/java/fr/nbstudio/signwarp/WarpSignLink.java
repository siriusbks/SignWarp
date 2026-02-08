package fr.nbstudio.signwarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarpSignLink {
    private static final String DB_URL = "jdbc:sqlite:" + JavaPlugin.getPlugin(SignWarp.class).getDataFolder() + File.separator + "warps.db";
    
    private final String warpName;
    private final Location signLocation;
    private final String signType; // "WARP" or "TARGET"
    
    public WarpSignLink(String warpName, Location signLocation, String signType) {
        this.warpName = warpName;
        this.signLocation = signLocation;
        this.signType = signType;
    }
    
    public String getWarpName() {
        return warpName;
    }
    
    public Location getSignLocation() {
        return signLocation;
    }
    
    public String getSignType() {
        return signType;
    }
    
    public void save() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO warp_sign_links (warp_name, sign_type, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                pstmt.setString(2, signType);
                pstmt.setString(3, signLocation.getWorld().getName());
                pstmt.setDouble(4, signLocation.getX());
                pstmt.setDouble(5, signLocation.getY());
                pstmt.setDouble(6, signLocation.getZ());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void remove() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "DELETE FROM warp_sign_links WHERE warp_name = ? AND sign_type = ? AND world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                pstmt.setString(2, signType);
                pstmt.setString(3, signLocation.getWorld().getName());
                pstmt.setDouble(4, signLocation.getX());
                pstmt.setDouble(5, signLocation.getY());
                pstmt.setDouble(6, signLocation.getZ());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static List<WarpSignLink> getByWarpName(String warpName) {
        List<WarpSignLink> links = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM warp_sign_links WHERE warp_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String signType = rs.getString("sign_type");
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        double z = rs.getDouble("z");
                        Location location = new Location(world, x, y, z);
                        links.add(new WarpSignLink(warpName, location, signType));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return links;
    }
    
    public static WarpSignLink getByLocation(Location location) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM warp_sign_links WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String warpName = rs.getString("warp_name");
                    String signType = rs.getString("sign_type");
                    return new WarpSignLink(warpName, location, signType);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Location getOtherEndLocation(String warpName, String currentSignType) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // If we're at a WARP sign, find the TARGET sign
            // If we're at a TARGET sign, find any WARP sign
            String targetType = currentSignType.equals("WARP") ? "TARGET" : "WARP";
            String sql = "SELECT * FROM warp_sign_links WHERE warp_name = ? AND sign_type = ? LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, warpName);
                pstmt.setString(2, targetType);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        double z = rs.getDouble("z");
                        return new Location(world, x, y, z);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void createTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "CREATE TABLE IF NOT EXISTS warp_sign_links (" +
                    "warp_name TEXT NOT NULL, " +
                    "sign_type TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "PRIMARY KEY (warp_name, sign_type, world, x, y, z))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void removeByLocation(Location location) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "DELETE FROM warp_sign_links WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setDouble(2, location.getX());
                pstmt.setDouble(3, location.getY());
                pstmt.setDouble(4, location.getZ());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
