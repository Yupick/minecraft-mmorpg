package com.nightslayer.mmorpg.api;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Admin API for web panel integration
 * 
 * Provides REST-like methods for:
 * - Player management
 * - Quest management
 * - Mob management
 * - Economy management
 * - Server statistics
 */
public class RPGAdminAPI {
    
    private final DatabaseManager dbManager;
    
    public RPGAdminAPI(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    // ==================== PLAYER MANAGEMENT ====================
    
    /**
     * Get all players with basic info
     */
    public List<Map<String, Object>> getPlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        String sql = "SELECT p.uuid, p.username, p.class, p.level, p.experience, pe.coins, " +
                     "p.strength, p.intelligence, p.dexterity, p.vitality " +
                     "FROM players p " +
                     "LEFT JOIN player_economy pe ON p.uuid = pe.uuid " +
                     "ORDER BY p.level DESC, p.experience DESC";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> player = new HashMap<>();
                player.put("uuid", rs.getString("uuid"));
                player.put("username", rs.getString("username"));
                player.put("class", rs.getString("class"));
                player.put("level", rs.getInt("level"));
                player.put("experience", rs.getInt("experience"));
                player.put("coins", rs.getInt("coins"));
                player.put("strength", rs.getInt("strength"));
                player.put("intelligence", rs.getInt("intelligence"));
                player.put("dexterity", rs.getInt("dexterity"));
                player.put("vitality", rs.getInt("vitality"));
                players.add(player);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting players", e);
        }
        
        return players;
    }
    
    /**
     * Get player stats by UUID
     */
    public Map<String, Object> getPlayerStats(String uuid) {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT * FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("uuid", rs.getString("uuid"));
                    stats.put("username", rs.getString("username"));
                    stats.put("class", rs.getString("class"));
                    stats.put("level", rs.getInt("level"));
                    stats.put("experience", rs.getInt("experience"));
                    stats.put("strength", rs.getInt("strength"));
                    stats.put("intelligence", rs.getInt("intelligence"));
                    stats.put("dexterity", rs.getInt("dexterity"));
                    stats.put("vitality", rs.getInt("vitality"));
                    stats.put("health", rs.getInt("health"));
                    stats.put("mana", rs.getInt("mana"));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting player stats", e);
        }
        
        return stats;
    }
    
    /**
     * Update player balance
     */
    public boolean updatePlayerBalance(String uuid, int coins) {
        String sql = "UPDATE player_economy SET coins = ? WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, coins);
            stmt.setString(2, uuid);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error updating balance", e);
            return false;
        }
    }
    
    /**
     * Update player level and experience
     */
    public boolean updatePlayerLevel(String uuid, int level, int experience) {
        String sql = "UPDATE players SET level = ?, experience = ? WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, level);
            stmt.setInt(2, experience);
            stmt.setString(3, uuid);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error updating level", e);
            return false;
        }
    }
    
    // ==================== QUEST MANAGEMENT ====================
    
    /**
     * Get all quests
     */
    public List<Map<String, Object>> getQuests() {
        List<Map<String, Object>> quests = new ArrayList<>();
        String sql = "SELECT * FROM quests";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getString("quest_id"));
                quest.put("name", rs.getString("name"));
                quest.put("description", rs.getString("description"));
                quest.put("type", rs.getString("type"));
                quest.put("target", rs.getString("target"));
                quest.put("target_amount", rs.getInt("target_amount"));
                quest.put("level_required", rs.getInt("level_required"));
                quest.put("coin_reward", rs.getInt("coin_reward"));
                quest.put("exp_reward", rs.getInt("exp_reward"));
                quests.add(quest);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting quests", e);
        }
        
        return quests;
    }
    
    /**
     * Create a new quest
     */
    public boolean createQuest(Map<String, Object> questData) {
        String sql = "INSERT INTO quests (quest_id, name, description, type, target, target_amount, " +
                     "level_required, coin_reward, exp_reward) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, (String) questData.get("quest_id"));
            stmt.setString(2, (String) questData.get("name"));
            stmt.setString(3, (String) questData.get("description"));
            stmt.setString(4, (String) questData.get("type"));
            stmt.setString(5, (String) questData.get("target"));
            stmt.setInt(6, (Integer) questData.get("target_amount"));
            stmt.setInt(7, (Integer) questData.get("level_required"));
            stmt.setInt(8, (Integer) questData.get("coin_reward"));
            stmt.setInt(9, (Integer) questData.get("exp_reward"));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error creating quest", e);
            return false;
        }
    }
    
    /**
     * Update an existing quest
     */
    public boolean updateQuest(String questId, Map<String, Object> questData) {
        String sql = "UPDATE quests SET name = ?, description = ?, type = ?, target = ?, " +
                     "target_amount = ?, level_required = ?, coin_reward = ?, exp_reward = ? " +
                     "WHERE quest_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, (String) questData.get("name"));
            stmt.setString(2, (String) questData.get("description"));
            stmt.setString(3, (String) questData.get("type"));
            stmt.setString(4, (String) questData.get("target"));
            stmt.setInt(5, (Integer) questData.get("target_amount"));
            stmt.setInt(6, (Integer) questData.get("level_required"));
            stmt.setInt(7, (Integer) questData.get("coin_reward"));
            stmt.setInt(8, (Integer) questData.get("exp_reward"));
            stmt.setString(9, questId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error updating quest", e);
            return false;
        }
    }
    
    // ==================== MOB MANAGEMENT ====================
    
    /**
     * Get all custom mobs
     */
    public List<Map<String, Object>> getMobs() {
        List<Map<String, Object>> mobs = new ArrayList<>();
        String sql = "SELECT * FROM custom_mobs";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> mob = new HashMap<>();
                mob.put("mob_id", rs.getString("mob_id"));
                mob.put("name", rs.getString("name"));
                mob.put("entity_type", rs.getString("entity_type"));
                mob.put("level", rs.getInt("level"));
                mob.put("health", rs.getDouble("health"));
                mob.put("damage", rs.getDouble("damage"));
                mob.put("exp_reward", rs.getInt("exp_reward"));
                mob.put("coin_reward", rs.getInt("coin_reward"));
                mobs.add(mob);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting mobs", e);
        }
        
        return mobs;
    }
    
    /**
     * Update a mob
     */
    public boolean updateMob(String mobId, Map<String, Object> mobData) {
        String sql = "UPDATE custom_mobs SET name = ?, entity_type = ?, level = ?, health = ?, " +
                     "damage = ?, exp_reward = ?, coin_reward = ? WHERE mob_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, (String) mobData.get("name"));
            stmt.setString(2, (String) mobData.get("entity_type"));
            stmt.setInt(3, (Integer) mobData.get("level"));
            stmt.setDouble(4, (Double) mobData.get("health"));
            stmt.setDouble(5, (Double) mobData.get("damage"));
            stmt.setInt(6, (Integer) mobData.get("exp_reward"));
            stmt.setInt(7, (Integer) mobData.get("coin_reward"));
            stmt.setString(8, mobId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error updating mob", e);
            return false;
        }
    }
    
    // ==================== SERVER STATISTICS ====================
    
    /**
     * Get economy statistics
     */
    public Map<String, Object> getEconomyStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) as total_players, " +
                     "SUM(coins) as total_coins, " +
                     "AVG(coins) as avg_coins, " +
                     "MAX(coins) as max_coins, " +
                     "MIN(coins) as min_coins " +
                     "FROM player_economy";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("total_players", rs.getInt("total_players"));
                stats.put("total_coins", rs.getLong("total_coins"));
                stats.put("avg_coins", rs.getDouble("avg_coins"));
                stats.put("max_coins", rs.getInt("max_coins"));
                stats.put("min_coins", rs.getInt("min_coins"));
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting economy stats", e);
        }
        
        return stats;
    }
    
    /**
     * Get server statistics
     */
    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Player stats
        String playerSql = "SELECT " +
                          "COUNT(*) as total_players, " +
                          "AVG(level) as avg_level, " +
                          "MAX(level) as max_level " +
                          "FROM players";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(playerSql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("total_players", rs.getInt("total_players"));
                stats.put("avg_level", rs.getDouble("avg_level"));
                stats.put("max_level", rs.getInt("max_level"));
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting player stats", e);
        }
        
        // Quest stats
        String questSql = "SELECT COUNT(*) as total_quests FROM quests";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(questSql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("total_quests", rs.getInt("total_quests"));
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting quest stats", e);
        }
        
        // Mob stats
        String mobSql = "SELECT COUNT(*) as total_mobs FROM custom_mobs";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(mobSql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("total_mobs", rs.getInt("total_mobs"));
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting mob stats", e);
        }
        
        // Add economy stats
        stats.putAll(getEconomyStats());
        
        return stats;
    }
    
    /**
     * Get recent transactions
     */
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("transaction_id", rs.getInt("transaction_id"));
                    transaction.put("from_uuid", rs.getString("from_uuid"));
                    transaction.put("to_uuid", rs.getString("to_uuid"));
                    transaction.put("amount", rs.getInt("amount"));
                    transaction.put("type", rs.getString("type"));
                    transaction.put("timestamp", rs.getString("timestamp"));
                    transactions.add(transaction);
                }
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG API] Error getting transactions", e);
        }
        
        return transactions;
    }
}
