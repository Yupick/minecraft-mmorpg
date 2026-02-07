package com.nightslayer.mmorpg.achievements;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages achievements and player progress tracking
 * 
 * Features:
 * - Load achievements from database
 * - Track player progress
 * - Award achievements
 * - Grant rewards
 */
public class AchievementManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<String, Achievement> achievements;
    
    public AchievementManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.achievements = new HashMap<>();
    }
    
    /**
     * Load all achievements from database
     */
    public void loadAchievements() {
        achievements.clear();
        String sql = "SELECT * FROM achievements_definitions";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getString("achievement_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getInt("target_value"),
                    rs.getInt("coin_reward"),
                    rs.getInt("exp_reward"),
                    rs.getString("icon")
                );
                achievements.put(achievement.getAchievementId(), achievement);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + achievements.size() + " achievements");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading achievements", e);
        }
    }
    
    /**
     * Get achievement by ID
     */
    public Achievement getAchievement(String achievementId) {
        return achievements.get(achievementId);
    }
    
    /**
     * Get all achievements for a player (with progress)
     */
    public List<Achievement> getPlayerAchievements(Player player) {
        return new ArrayList<>(achievements.values());
    }
    
    /**
     * Track progress for an achievement
     */
    public void trackProgress(Player player, String type, int increment) {
        // Find all achievements of this type
        for (Achievement achievement : achievements.values()) {
            if (achievement.getType().equalsIgnoreCase(type)) {
                updateProgress(player, achievement.getAchievementId(), increment);
            }
        }
    }
    
    /**
     * Update progress for specific achievement
     */
    private void updateProgress(Player player, String achievementId, int increment) {
        // Check if already completed
        if (hasAchievement(player, achievementId)) {
            return;
        }
        
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            return;
        }
        
        // Get current progress
        int currentProgress = getProgress(player, achievementId);
        int newProgress = currentProgress + increment;
        
        // Update progress
        String sql = "INSERT OR REPLACE INTO player_achievements (uuid, achievement_id, progress, unlocked) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, achievementId);
            stmt.setInt(3, newProgress);
            stmt.setBoolean(4, false);
            stmt.executeUpdate();
            
            // Check if achievement is completed
            if (newProgress >= achievement.getTargetValue()) {
                unlockAchievement(player, achievementId);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error updating achievement progress", e);
        }
    }
    
    /**
     * Unlock an achievement
     */
    private void unlockAchievement(Player player, String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            return;
        }
        
        // Mark as unlocked
        String sql = "UPDATE player_achievements SET unlocked = 1, unlock_date = datetime('now') WHERE uuid = ? AND achievement_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, achievementId);
            stmt.executeUpdate();
            
            // Give rewards
            giveRewards(player, achievement);
            
            // Announce
            Bukkit.broadcastMessage(langManager.getMessage("achievement.unlocked",
                player.getName(), achievement.getName()));
            
            player.sendMessage(langManager.getMessage("achievement.reward",
                achievement.getCoinReward(), achievement.getExpReward()));
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error unlocking achievement", e);
        }
    }
    
    /**
     * Give rewards for achievement
     */
    private void giveRewards(Player player, Achievement achievement) {
        String updateCoins = "UPDATE player_economy SET coins = coins + ? WHERE uuid = ?";
        String updateExp = "UPDATE players SET experience = experience + ? WHERE uuid = ?";
        
        try (PreparedStatement stmt1 = dbManager.getConnection().prepareStatement(updateCoins);
             PreparedStatement stmt2 = dbManager.getConnection().prepareStatement(updateExp)) {
            
            stmt1.setInt(1, achievement.getCoinReward());
            stmt1.setString(2, player.getUniqueId().toString());
            stmt1.executeUpdate();
            
            stmt2.setInt(1, achievement.getExpReward());
            stmt2.setString(2, player.getUniqueId().toString());
            stmt2.executeUpdate();
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error giving achievement rewards", e);
        }
    }
    
    /**
     * Check if player has achievement
     */
    private boolean hasAchievement(Player player, String achievementId) {
        String sql = "SELECT unlocked FROM player_achievements WHERE uuid = ? AND achievement_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, achievementId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("unlocked");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error checking achievement", e);
        }
        
        return false;
    }
    
    /**
     * Get player progress for achievement
     */
    private int getProgress(Player player, String achievementId) {
        String sql = "SELECT progress FROM player_achievements WHERE uuid = ? AND achievement_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, achievementId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("progress");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting progress", e);
        }
        
        return 0;
    }
    
    /**
     * Get player's achievement stats
     */
    public Map<String, Object> getPlayerStats(Player player) {
        Map<String, Object> stats = new HashMap<>();
        
        int total = achievements.size();
        int unlocked = 0;
        
        for (Achievement achievement : achievements.values()) {
            if (hasAchievement(player, achievement.getAchievementId())) {
                unlocked++;
            }
        }
        
        stats.put("total", total);
        stats.put("unlocked", unlocked);
        stats.put("percentage", total > 0 ? (unlocked * 100 / total) : 0);
        
        return stats;
    }
    
    /**
     * Inner class representing an achievement
     */
    public static class Achievement {
        private final String achievementId;
        private final String name;
        private final String description;
        private final String type;
        private final int targetValue;
        private final int coinReward;
        private final int expReward;
        private final String icon;
        
        public Achievement(String achievementId, String name, String description, String type,
                          int targetValue, int coinReward, int expReward, String icon) {
            this.achievementId = achievementId;
            this.name = name;
            this.description = description;
            this.type = type;
            this.targetValue = targetValue;
            this.coinReward = coinReward;
            this.expReward = expReward;
            this.icon = icon;
        }
        
        // Getters
        public String getAchievementId() { return achievementId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public int getTargetValue() { return targetValue; }
        public int getCoinReward() { return coinReward; }
        public int getExpReward() { return expReward; }
        public String getIcon() { return icon; }
    }
}
