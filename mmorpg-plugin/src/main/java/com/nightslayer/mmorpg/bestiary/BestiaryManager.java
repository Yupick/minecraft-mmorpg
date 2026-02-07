package com.nightslayer.mmorpg.bestiary;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages bestiary system - mob kill tracking
 * 
 * Features:
 * - Track mob kills per player
 * - Statistics per mob type
 * - Completion rewards
 * - Bestiary progression
 */
public class BestiaryManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<EntityType, BestiaryEntry> entries;
    
    public BestiaryManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.entries = new HashMap<>();
    }
    
    /**
     * Initialize bestiary entries for all hostile mobs
     */
    public void initializeBestiary() {
        entries.clear();
        
        // Define tracked mobs with kill requirements
        registerEntry(EntityType.ZOMBIE, "Zombie", 100, 50, 100);
        registerEntry(EntityType.SKELETON, "Esqueleto", 100, 50, 100);
        registerEntry(EntityType.SPIDER, "Araña", 100, 50, 100);
        registerEntry(EntityType.CREEPER, "Creeper", 50, 100, 200);
        registerEntry(EntityType.ENDERMAN, "Enderman", 25, 150, 300);
        registerEntry(EntityType.BLAZE, "Blaze", 50, 150, 300);
        registerEntry(EntityType.WITCH, "Bruja", 25, 200, 400);
        registerEntry(EntityType.ENDER_DRAGON, "Dragón del End", 1, 5000, 10000);
        registerEntry(EntityType.WITHER, "Wither", 1, 5000, 10000);
        registerEntry(EntityType.PHANTOM, "Phantom", 50, 100, 200);
        registerEntry(EntityType.PILLAGER, "Saqueador", 50, 100, 200);
        registerEntry(EntityType.RAVAGER, "Devastador", 10, 500, 1000);
        
        Bukkit.getLogger().info("[MMORPG] Initialized bestiary with " + entries.size() + " entries");
    }
    
    /**
     * Register a bestiary entry
     */
    private void registerEntry(EntityType type, String name, int killsRequired, int coinReward, int expReward) {
        entries.put(type, new BestiaryEntry(type, name, killsRequired, coinReward, expReward));
    }
    
    /**
     * Record a mob kill
     */
    public void recordKill(Player player, EntityType mobType) {
        if (!entries.containsKey(mobType)) {
            return; // Not tracked in bestiary
        }
        
        // Get current kills
        int currentKills = getKillCount(player, mobType);
        int newKills = currentKills + 1;
        
        // Update kills
        String sql = "INSERT OR REPLACE INTO player_bestiary (uuid, mob_type, kills, completed) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, mobType.name());
            stmt.setInt(3, newKills);
            stmt.setBoolean(4, false);
            stmt.executeUpdate();
            
            // Check for completion
            BestiaryEntry entry = entries.get(mobType);
            if (entry != null && newKills >= entry.getKillsRequired()) {
                completeBestiaryEntry(player, mobType);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error recording bestiary kill", e);
        }
    }
    
    /**
     * Complete a bestiary entry
     */
    private void completeBestiaryEntry(Player player, EntityType mobType) {
        // Check if already completed
        if (isCompleted(player, mobType)) {
            return;
        }
        
        BestiaryEntry entry = entries.get(mobType);
        if (entry == null) {
            return;
        }
        
        // Mark as completed
        String sql = "UPDATE player_bestiary SET completed = 1, completion_date = datetime('now') WHERE uuid = ? AND mob_type = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, mobType.name());
            stmt.executeUpdate();
            
            // Give rewards
            giveRewards(player, entry);
            
            player.sendMessage(langManager.getMessage("bestiary.completed",
                entry.getName(), entry.getCoinReward(), entry.getExpReward()));
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error completing bestiary entry", e);
        }
    }
    
    /**
     * Give completion rewards
     */
    private void giveRewards(Player player, BestiaryEntry entry) {
        String updateCoins = "UPDATE player_economy SET coins = coins + ? WHERE uuid = ?";
        String updateExp = "UPDATE players SET experience = experience + ? WHERE uuid = ?";
        
        try (PreparedStatement stmt1 = dbManager.getConnection().prepareStatement(updateCoins);
             PreparedStatement stmt2 = dbManager.getConnection().prepareStatement(updateExp)) {
            
            stmt1.setInt(1, entry.getCoinReward());
            stmt1.setString(2, player.getUniqueId().toString());
            stmt1.executeUpdate();
            
            stmt2.setInt(1, entry.getExpReward());
            stmt2.setString(2, player.getUniqueId().toString());
            stmt2.executeUpdate();
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error giving bestiary rewards", e);
        }
    }
    
    /**
     * Get kill count for a mob type
     */
    public int getKillCount(Player player, EntityType mobType) {
        String sql = "SELECT kills FROM player_bestiary WHERE uuid = ? AND mob_type = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, mobType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("kills");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting kill count", e);
        }
        
        return 0;
    }
    
    /**
     * Check if entry is completed
     */
    private boolean isCompleted(Player player, EntityType mobType) {
        String sql = "SELECT completed FROM player_bestiary WHERE uuid = ? AND mob_type = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, mobType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("completed");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error checking completion", e);
        }
        
        return false;
    }
    
    /**
     * Get player's bestiary stats
     */
    public Map<String, Object> getPlayerStats(Player player) {
        Map<String, Object> stats = new HashMap<>();
        
        int totalEntries = entries.size();
        int completed = 0;
        int totalKills = 0;
        
        for (EntityType type : entries.keySet()) {
            int kills = getKillCount(player, type);
            totalKills += kills;
            
            if (isCompleted(player, type)) {
                completed++;
            }
        }
        
        stats.put("total", totalEntries);
        stats.put("completed", completed);
        stats.put("total_kills", totalKills);
        stats.put("percentage", totalEntries > 0 ? (completed * 100 / totalEntries) : 0);
        
        return stats;
    }
    
    /**
     * Get all bestiary entries with player progress
     */
    public List<Map<String, Object>> getPlayerBestiary(Player player) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (BestiaryEntry entry : entries.values()) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", entry.getName());
            data.put("type", entry.getMobType().name());
            data.put("kills", getKillCount(player, entry.getMobType()));
            data.put("kills_required", entry.getKillsRequired());
            data.put("completed", isCompleted(player, entry.getMobType()));
            data.put("coin_reward", entry.getCoinReward());
            data.put("exp_reward", entry.getExpReward());
            
            result.add(data);
        }
        
        return result;
    }
    
    /**
     * Inner class representing a bestiary entry
     */
    public static class BestiaryEntry {
        private final EntityType mobType;
        private final String name;
        private final int killsRequired;
        private final int coinReward;
        private final int expReward;
        
        public BestiaryEntry(EntityType mobType, String name, int killsRequired, int coinReward, int expReward) {
            this.mobType = mobType;
            this.name = name;
            this.killsRequired = killsRequired;
            this.coinReward = coinReward;
            this.expReward = expReward;
        }
        
        // Getters
        public EntityType getMobType() { return mobType; }
        public String getName() { return name; }
        public int getKillsRequired() { return killsRequired; }
        public int getCoinReward() { return coinReward; }
        public int getExpReward() { return expReward; }
    }
}
