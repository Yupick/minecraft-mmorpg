package com.nightslayer.mmorpg.respawn;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages respawn zones and respawn mechanics
 * 
 * Features:
 * - Load respawn zones from database
 * - Teleport players to respawn
 * - Temporary invulnerability after respawn
 * - Zone-based respawning
 */
public class RespawnManager {
    
    private final DatabaseManager dbManager;
    private final Map<String, RespawnZone> zones;
    private final Set<UUID> invulnerablePlayers;
    
    public RespawnManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.zones = new HashMap<>();
        this.invulnerablePlayers = new HashSet<>();
    }
    
    /**
     * Load all respawn zones from database
     */
    public void loadZones() {
        zones.clear();
        String sql = "SELECT * FROM respawn_zones";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                RespawnZone zone = new RespawnZone(
                    rs.getString("zone_id"),
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getFloat("yaw"),
                    rs.getFloat("pitch"),
                    rs.getInt("invulnerability_seconds"),
                    rs.getBoolean("is_default")
                );
                zones.put(zone.getZoneId(), zone);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + zones.size() + " respawn zones");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading respawn zones", e);
        }
    }
    
    /**
     * Get a respawn zone by ID
     */
    public RespawnZone getZone(String zoneId) {
        return zones.get(zoneId);
    }
    
    /**
     * Get default respawn zone
     */
    public RespawnZone getDefaultZone() {
        for (RespawnZone zone : zones.values()) {
            if (zone.isDefault()) {
                return zone;
            }
        }
        return zones.values().isEmpty() ? null : zones.values().iterator().next();
    }
    
    /**
     * Respawn player at their assigned zone or default
     */
    public void respawnPlayer(Player player) {
        // Get player's assigned zone or use default
        String assignedZoneId = getPlayerAssignedZone(player);
        RespawnZone zone = assignedZoneId != null ? zones.get(assignedZoneId) : getDefaultZone();
        
        if (zone == null) {
            Bukkit.getLogger().warning("[MMORPG] No respawn zone found for " + player.getName());
            return;
        }
        
        // Teleport to respawn location
        Location loc = zone.getLocation();
        if (loc != null) {
            player.teleport(loc);
            
            // Apply invulnerability
            if (zone.getInvulnerabilitySeconds() > 0) {
                makeInvulnerable(player, zone.getInvulnerabilitySeconds());
            }
            
            // Restore health and hunger
            if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
            player.setFoodLevel(20);
            player.setFireTicks(0);
        }
    }
    
    /**
     * Make player invulnerable for specified seconds
     */
    private void makeInvulnerable(Player player, int seconds) {
        invulnerablePlayers.add(player.getUniqueId());
        
        // Apply resistance effect
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, 
            seconds * 20, 
            4, 
            false, 
            true
        ));
        
        // Schedule removal of invulnerability
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("MMORPGPlugin"),
            () -> invulnerablePlayers.remove(player.getUniqueId()),
            seconds * 20L
        );
    }
    
    /**
     * Check if player is currently invulnerable
     */
    public boolean isInvulnerable(Player player) {
        return invulnerablePlayers.contains(player.getUniqueId());
    }
    
    /**
     * Get player's assigned respawn zone
     */
    private String getPlayerAssignedZone(Player player) {
        String sql = "SELECT respawn_zone FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("respawn_zone");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player respawn zone", e);
        }
        
        return null;
    }
    
    /**
     * Set player's respawn zone
     */
    public void setPlayerRespawnZone(Player player, String zoneId) {
        String sql = "UPDATE players SET respawn_zone = ? WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, zoneId);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error setting player respawn zone", e);
        }
    }
    
    /**
     * Inner class representing a respawn zone
     */
    public static class RespawnZone {
        private final String zoneId;
        private final String name;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final int invulnerabilitySeconds;
        private final boolean isDefault;
        
        public RespawnZone(String zoneId, String name, String worldName, double x, double y, double z,
                          float yaw, float pitch, int invulnerabilitySeconds, boolean isDefault) {
            this.zoneId = zoneId;
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.invulnerabilitySeconds = invulnerabilitySeconds;
            this.isDefault = isDefault;
        }
        
        /**
         * Get the Bukkit Location object
         */
        public Location getLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }
        
        // Getters
        public String getZoneId() { return zoneId; }
        public String getName() { return name; }
        public String getWorldName() { return worldName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public int getInvulnerabilitySeconds() { return invulnerabilitySeconds; }
        public boolean isDefault() { return isDefault; }
    }
}
