package com.nightslayer.mmorpg.spawns;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages custom spawn points and spawn rates
 * 
 * Features:
 * - Custom spawn points
 * - Biome-based spawning
 * - Spawn rate control
 * - Custom mob spawning
 */
public class SpawnManager implements Listener {
    
    private final DatabaseManager dbManager;
    private final Map<String, SpawnPoint> spawnPoints;
    private final Random random;
    
    public SpawnManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.spawnPoints = new HashMap<>();
        this.random = new Random();
    }
    
    /**
     * Load spawn points from database
     */
    public void loadSpawnPoints() {
        spawnPoints.clear();
        String sql = "SELECT * FROM spawn_points";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                SpawnPoint sp = new SpawnPoint(
                    rs.getString("spawn_id"),
                    rs.getString("world"),
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getInt("radius"),
                    rs.getString("mob_type"),
                    rs.getInt("max_mobs"),
                    rs.getInt("spawn_interval"),
                    rs.getBoolean("active")
                );
                spawnPoints.put(sp.getSpawnId(), sp);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + spawnPoints.size() + " spawn points");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading spawn points", e);
        }
    }
    
    /**
     * Start spawn timers for all active spawn points
     */
    public void startSpawnTimers() {
        for (SpawnPoint sp : spawnPoints.values()) {
            if (sp.isActive()) {
                scheduleSpawn(sp);
            }
        }
    }
    
    /**
     * Schedule spawn for a spawn point
     */
    private void scheduleSpawn(SpawnPoint sp) {
        long interval = sp.getSpawnInterval() * 20L; // Convert seconds to ticks
        
        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("MMORPGPlugin"),
            () -> trySpawn(sp),
            interval,
            interval
        );
    }
    
    /**
     * Try to spawn a mob at spawn point
     */
    private void trySpawn(SpawnPoint sp) {
        if (!sp.isActive()) {
            return;
        }
        
        // Check if max mobs reached
        int currentMobs = countMobsNear(sp);
        if (currentMobs >= sp.getMaxMobs()) {
            return;
        }
        
        // Get random location within radius
        Location center = sp.getLocation();
        if (center == null) {
            return;
        }
        
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * sp.getRadius();
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);
        
        // Find safe y coordinate
        spawnLoc = findSafeLocation(spawnLoc);
        if (spawnLoc == null) {
            return;
        }
        
        // Spawn mob
        EntityType type = EntityType.valueOf(sp.getMobType().toUpperCase());
        center.getWorld().spawnEntity(spawnLoc, type);
    }
    
    /**
     * Count mobs near spawn point
     */
    private int countMobsNear(SpawnPoint sp) {
        Location center = sp.getLocation();
        if (center == null) {
            return 0;
        }
        
        EntityType type = EntityType.valueOf(sp.getMobType().toUpperCase());
        double radius = sp.getRadius();
        
        return (int) center.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
            .filter(entity -> entity.getType() == type)
            .count();
    }
    
    /**
     * Find safe location for spawning
     */
    private Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        
        // Find highest solid block
        for (int y = world.getMaxHeight() - 1; y > world.getMinHeight(); y--) {
            Location checkLoc = new Location(world, x, y, z);
            
            if (checkLoc.getBlock().getType().isSolid()) {
                // Check if there's space above
                Location aboveLoc = checkLoc.clone().add(0, 1, 0);
                Location above2Loc = checkLoc.clone().add(0, 2, 0);
                
                if (!aboveLoc.getBlock().getType().isSolid() && 
                    !above2Loc.getBlock().getType().isSolid()) {
                    return aboveLoc;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create a new spawn point
     */
    public void createSpawnPoint(String spawnId, Location loc, String mobType, 
                                int radius, int maxMobs, int spawnInterval) {
        String sql = "INSERT INTO spawn_points (spawn_id, world, x, y, z, radius, mob_type, max_mobs, spawn_interval, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, spawnId);
            stmt.setString(2, loc.getWorld().getName());
            stmt.setDouble(3, loc.getX());
            stmt.setDouble(4, loc.getY());
            stmt.setDouble(5, loc.getZ());
            stmt.setInt(6, radius);
            stmt.setString(7, mobType);
            stmt.setInt(8, maxMobs);
            stmt.setInt(9, spawnInterval);
            stmt.executeUpdate();
            
            // Reload spawn points
            loadSpawnPoints();
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error creating spawn point", e);
        }
    }
    
    /**
     * Delete a spawn point
     */
    public void deleteSpawnPoint(String spawnId) {
        String sql = "DELETE FROM spawn_points WHERE spawn_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, spawnId);
            stmt.executeUpdate();
            
            spawnPoints.remove(spawnId);
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error deleting spawn point", e);
        }
    }
    
    /**
     * Toggle spawn point active status
     */
    public void toggleSpawnPoint(String spawnId, boolean active) {
        String sql = "UPDATE spawn_points SET active = ? WHERE spawn_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setString(2, spawnId);
            stmt.executeUpdate();
            
            SpawnPoint sp = spawnPoints.get(spawnId);
            if (sp != null) {
                sp.setActive(active);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error toggling spawn point", e);
        }
    }
    
    /**
     * Handle natural spawns - can be used to control spawn rates
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Can add logic here to control natural spawns
        // For example, prevent spawns in certain areas, or modify spawn rates
    }
    
    /**
     * Inner class representing a spawn point
     */
    public static class SpawnPoint {
        private final String spawnId;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private final int radius;
        private final String mobType;
        private final int maxMobs;
        private final int spawnInterval;
        private boolean active;
        
        public SpawnPoint(String spawnId, String worldName, double x, double y, double z,
                         int radius, String mobType, int maxMobs, int spawnInterval, boolean active) {
            this.spawnId = spawnId;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.mobType = mobType;
            this.maxMobs = maxMobs;
            this.spawnInterval = spawnInterval;
            this.active = active;
        }
        
        public Location getLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z);
        }
        
        // Getters and setters
        public String getSpawnId() { return spawnId; }
        public String getWorldName() { return worldName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public int getRadius() { return radius; }
        public String getMobType() { return mobType; }
        public int getMaxMobs() { return maxMobs; }
        public int getSpawnInterval() { return spawnInterval; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
