package com.nightslayer.mmorpg.invasions;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages scheduled invasions and events
 * 
 * Features:
 * - Load invasions from database
 * - Schedule automatic invasions
 * - Wave-based mob spawning
 * - Server-wide participation
 * - Rewards for participants
 */
public class InvasionManager {
    
    private final DatabaseManager dbManager;
    private final Map<String, Invasion> invasions;
    private ActiveInvasion currentInvasion;
    private final Set<UUID> participants;
    
    public InvasionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.invasions = new HashMap<>();
        this.participants = new HashSet<>();
    }
    
    /**
     * Load all invasions from database
     */
    public void loadInvasions() {
        invasions.clear();
        String sql = "SELECT * FROM invasions";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Invasion invasion = new Invasion(
                    rs.getString("invasion_id"),
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getDouble("center_x"),
                    rs.getDouble("center_y"),
                    rs.getDouble("center_z"),
                    rs.getInt("radius"),
                    rs.getInt("waves"),
                    rs.getString("mob_types"),
                    rs.getInt("mobs_per_wave"),
                    rs.getInt("interval_minutes"),
                    rs.getInt("coin_reward_per_kill"),
                    rs.getInt("exp_reward_per_kill")
                );
                invasions.put(invasion.getInvasionId(), invasion);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + invasions.size() + " invasions");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading invasions", e);
        }
    }
    
    /**
     * Schedule automatic invasions
     */
    public void scheduleInvasions() {
        for (Invasion invasion : invasions.values()) {
            long interval = invasion.getIntervalMinutes() * 60 * 20L; // Convert to ticks
            
            Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("MMORPGPlugin"),
                () -> startInvasion(invasion.getInvasionId()),
                interval, // Initial delay
                interval  // Period
            );
        }
    }
    
    /**
     * Start an invasion
     */
    public boolean startInvasion(String invasionId) {
        if (currentInvasion != null) {
            return false; // Already an active invasion
        }
        
        Invasion invasion = invasions.get(invasionId);
        if (invasion == null) {
            return false;
        }
        
        currentInvasion = new ActiveInvasion(invasion);
        participants.clear();
        
        // Announce invasion
        Bukkit.broadcastMessage("§c§l[INVASIÓN] §f" + invasion.getName() + " ha comenzado!");
        Bukkit.broadcastMessage("§7Defiende el servidor y gana recompensas!");
        
        // Start first wave
        currentInvasion.startNextWave();
        
        return true;
    }
    
    /**
     * End current invasion
     */
    public void endInvasion(boolean success) {
        if (currentInvasion == null) return;
        
        if (success) {
            Bukkit.broadcastMessage("§a§l[INVASIÓN] §f¡Invasión repelida con éxito!");
            
            // Distribute participation rewards
            for (UUID uuid : participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    int bonusCoins = 1000;
                    int bonusExp = 500;
                    giveReward(player, bonusCoins, bonusExp);
                    player.sendMessage("§a¡Recompensa de participación: " + bonusCoins + " monedas, " + bonusExp + " EXP!");
                }
            }
        } else {
            Bukkit.broadcastMessage("§c§l[INVASIÓN] §fLa invasión ha sido demasiado fuerte...");
        }
        
        currentInvasion = null;
        participants.clear();
    }
    
    /**
     * Handle mob death during invasion
     */
    public void handleMobDeath(LivingEntity entity, Player killer) {
        if (currentInvasion == null) return;
        
        // Check if mob is part of invasion
        if (!currentInvasion.isInvasionMob(entity)) return;
        
        // Track participant
        participants.add(killer.getUniqueId());
        
        // Give kill rewards
        Invasion invasion = currentInvasion.getInvasion();
        giveReward(killer, invasion.getCoinRewardPerKill(), invasion.getExpRewardPerKill());
        
        // Update invasion state
        currentInvasion.onMobKilled(entity);
    }
    
    /**
     * Give rewards to player
     */
    private void giveReward(Player player, int coins, int exp) {
        String updateCoins = "UPDATE player_economy SET coins = coins + ? WHERE uuid = ?";
        String updateExp = "UPDATE players SET experience = experience + ? WHERE uuid = ?";
        
        try (PreparedStatement stmt1 = dbManager.getConnection().prepareStatement(updateCoins);
             PreparedStatement stmt2 = dbManager.getConnection().prepareStatement(updateExp)) {
            
            stmt1.setInt(1, coins);
            stmt1.setString(2, player.getUniqueId().toString());
            stmt1.executeUpdate();
            
            stmt2.setInt(1, exp);
            stmt2.setString(2, player.getUniqueId().toString());
            stmt2.executeUpdate();
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error giving invasion reward", e);
        }
    }
    
    /**
     * Get current active invasion
     */
    public ActiveInvasion getCurrentInvasion() {
        return currentInvasion;
    }
    
    /**
     * Inner class representing invasion definition
     */
    public static class Invasion {
        private final String invasionId;
        private final String name;
        private final String worldName;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final int radius;
        private final int waves;
        private final String mobTypes;
        private final int mobsPerWave;
        private final int intervalMinutes;
        private final int coinRewardPerKill;
        private final int expRewardPerKill;
        
        public Invasion(String invasionId, String name, String worldName, double centerX, double centerY,
                       double centerZ, int radius, int waves, String mobTypes, int mobsPerWave,
                       int intervalMinutes, int coinRewardPerKill, int expRewardPerKill) {
            this.invasionId = invasionId;
            this.name = name;
            this.worldName = worldName;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.waves = waves;
            this.mobTypes = mobTypes;
            this.mobsPerWave = mobsPerWave;
            this.intervalMinutes = intervalMinutes;
            this.coinRewardPerKill = coinRewardPerKill;
            this.expRewardPerKill = expRewardPerKill;
        }
        
        public Location getCenterLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, centerX, centerY, centerZ);
        }
        
        // Getters
        public String getInvasionId() { return invasionId; }
        public String getName() { return name; }
        public int getWaves() { return waves; }
        public String getMobTypes() { return mobTypes; }
        public int getMobsPerWave() { return mobsPerWave; }
        public int getRadius() { return radius; }
        public int getIntervalMinutes() { return intervalMinutes; }
        public int getCoinRewardPerKill() { return coinRewardPerKill; }
        public int getExpRewardPerKill() { return expRewardPerKill; }
    }
    
    /**
     * Inner class representing active invasion instance
     */
    public class ActiveInvasion {
        private final Invasion invasion;
        private int currentWave;
        private final Set<UUID> spawnedMobs;
        
        public ActiveInvasion(Invasion invasion) {
            this.invasion = invasion;
            this.currentWave = 0;
            this.spawnedMobs = new HashSet<>();
        }
        
        /**
         * Start next wave
         */
        public void startNextWave() {
            currentWave++;
            
            if (currentWave > invasion.getWaves()) {
                // All waves complete
                endInvasion(true);
                return;
            }
            
            // Announce wave
            Bukkit.broadcastMessage("§e§l[INVASIÓN] §fOleada " + currentWave + "/" + invasion.getWaves());
            
            // Spawn mobs
            spawnWaveMobs();
        }
        
        /**
         * Spawn mobs for current wave
         */
        private void spawnWaveMobs() {
            Location center = invasion.getCenterLocation();
            if (center == null) return;
            
            String[] mobTypes = invasion.getMobTypes().split(",");
            int radius = invasion.getRadius();
            
            for (int i = 0; i < invasion.getMobsPerWave(); i++) {
                // Random mob type
                String mobType = mobTypes[(int) (Math.random() * mobTypes.length)].trim();
                EntityType type = EntityType.valueOf(mobType.toUpperCase());
                
                // Random location within radius
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * radius;
                double x = center.getX() + distance * Math.cos(angle);
                double z = center.getZ() + distance * Math.sin(angle);
                
                Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);
                LivingEntity entity = (LivingEntity) center.getWorld().spawnEntity(spawnLoc, type);
                
                // Mark as invasion mob
                entity.setCustomName("§c[INVASIÓN] " + entity.getType().name());
                entity.setCustomNameVisible(true);
                
                spawnedMobs.add(entity.getUniqueId());
            }
        }
        
        /**
         * Check if entity is part of this invasion
         */
        public boolean isInvasionMob(LivingEntity entity) {
            return spawnedMobs.contains(entity.getUniqueId());
        }
        
        /**
         * Handle mob death
         */
        public void onMobKilled(LivingEntity entity) {
            spawnedMobs.remove(entity.getUniqueId());
            
            // Check if wave is complete
            if (spawnedMobs.isEmpty()) {
                // Start next wave after delay
                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("MMORPGPlugin"),
                    this::startNextWave,
                    100L // 5 seconds
                );
            }
        }
        
        // Getters
        public Invasion getInvasion() { return invasion; }
        public int getCurrentWave() { return currentWave; }
    }
}
