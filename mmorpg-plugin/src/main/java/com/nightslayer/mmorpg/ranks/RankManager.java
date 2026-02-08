package com.nightslayer.mmorpg.ranks;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages player ranks and progression system
 * 
 * Features:
 * - Rank progression based on level/achievements
 * - Benefits per rank
 * - Rank ascension system
 * - Prestige levels
 */
public class RankManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final List<Rank> ranks;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    
    public RankManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.ranks = new ArrayList<>();
        initializeRanks();
    }
    
    /**
     * Initialize rank definitions
     */
    private void initializeRanks() {
        ranks.clear();
        
        // Define ranks with requirements and benefits
        ranks.add(new Rank("novice", "Novato", 1, "§f", 0, 0, 1.0, 1.0));
        ranks.add(new Rank("apprentice", "Aprendiz", 10, "§a", 500, 1000, 1.05, 1.05));
        ranks.add(new Rank("adept", "Adepto", 20, "§2", 2000, 5000, 1.10, 1.10));
        ranks.add(new Rank("expert", "Experto", 30, "§b", 5000, 10000, 1.15, 1.15));
        ranks.add(new Rank("master", "Maestro", 40, "§9", 10000, 20000, 1.20, 1.20));
        ranks.add(new Rank("grandmaster", "Gran Maestro", 50, "§5", 20000, 50000, 1.30, 1.30));
        ranks.add(new Rank("legend", "Leyenda", 60, "§6", 50000, 100000, 1.50, 1.50));
        ranks.add(new Rank("mythic", "Mítico", 75, "§c", 100000, 250000, 2.0, 2.0));
        ranks.add(new Rank("divine", "Divino", 100, "§d", 500000, 1000000, 3.0, 3.0));
        
        Bukkit.getLogger().info("[MMORPG] Initialized " + ranks.size() + " ranks");
    }
    
    /**
     * Get rank by ID
     */
    public Rank getRank(String rankId) {
        for (Rank rank : ranks) {
            if (rank.getRankId().equals(rankId)) {
                return rank;
            }
        }
        return ranks.get(0); // Default to novice
    }
    
    /**
     * Get player's current rank
     */
    public Rank getPlayerRank(Player player) {
        String sql = "SELECT rank_id FROM player_ranks WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getRank(rs.getString("rank_id"));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player rank", e);
        }
        
        return ranks.get(0); // Default to novice
    }
    
    /**
     * Check if player can ascend to next rank
     */
    public boolean canAscend(Player player) {
        Rank currentRank = getPlayerRank(player);
        Rank nextRank = getNextRank(currentRank);
        
        if (nextRank == null) {
            return false; // Already at max rank
        }
        
        // Check level requirement
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < nextRank.getLevelRequired()) {
            return false;
        }
        
        // Check coin requirement
        int playerBalance = getPlayerBalance(player);
        if (playerBalance < nextRank.getCoinCost()) {
            return false;
        }
        
        // Check exp requirement
        int playerExp = getPlayerExp(player);
        if (playerExp < nextRank.getExpCost()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Ascend player to next rank
     */
    public boolean ascendPlayer(Player player) {
        if (!canAscend(player)) {
            player.sendMessage(langManager.getMessage("rank.cannot_ascend"));
            return false;
        }
        
        Rank currentRank = getPlayerRank(player);
        Rank nextRank = getNextRank(currentRank);
        
        if (nextRank == null) {
            return false;
        }
        
        // Charge costs
        chargeCoins(player, nextRank.getCoinCost());
        chargeExp(player, nextRank.getExpCost());
        
        // Update rank
        String sql = "INSERT OR REPLACE INTO player_ranks (uuid, rank_id, ascension_date) VALUES (?, ?, datetime('now'))";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, nextRank.getRankId());
            stmt.executeUpdate();
            
            // Announce ascension
            Bukkit.broadcast(LEGACY.deserialize(langManager.getMessage("rank.ascended",
                player.getName(), nextRank.getColorCode() + nextRank.getName())));
            
            player.sendMessage(langManager.getMessage("rank.benefits",
                (int)((nextRank.getDamageMultiplier() - 1) * 100),
                (int)((nextRank.getExpMultiplier() - 1) * 100)));
            
            return true;
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error ascending player", e);
            return false;
        }
    }
    
    /**
     * Get next rank after current
     */
    private Rank getNextRank(Rank current) {
        int currentIndex = ranks.indexOf(current);
        if (currentIndex >= 0 && currentIndex < ranks.size() - 1) {
            return ranks.get(currentIndex + 1);
        }
        return null;
    }
    
    /**
     * Apply rank multipliers to damage
     */
    public double applyDamageMultiplier(Player player, double baseDamage) {
        Rank rank = getPlayerRank(player);
        return baseDamage * rank.getDamageMultiplier();
    }
    
    /**
     * Apply rank multipliers to exp
     */
    public int applyExpMultiplier(Player player, int baseExp) {
        Rank rank = getPlayerRank(player);
        return (int)(baseExp * rank.getExpMultiplier());
    }
    
    // Helper methods
    private int getPlayerLevel(Player player) {
        String sql = "SELECT level FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("level");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player level", e);
        }
        return 1;
    }
    
    private int getPlayerBalance(Player player) {
        String sql = "SELECT coins FROM player_economy WHERE uuid = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("coins");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting balance", e);
        }
        return 0;
    }
    
    private int getPlayerExp(Player player) {
        String sql = "SELECT experience FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("experience");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting exp", e);
        }
        return 0;
    }
    
    private void chargeCoins(Player player, int amount) {
        String sql = "UPDATE player_economy SET coins = coins - ? WHERE uuid = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error charging coins", e);
        }
    }
    
    private void chargeExp(Player player, int amount) {
        String sql = "UPDATE players SET experience = experience - ? WHERE uuid = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error charging exp", e);
        }
    }
    
    /**
     * Inner class representing a rank
     */
    public static class Rank {
        private final String rankId;
        private final String name;
        private final int levelRequired;
        private final String colorCode;
        private final int coinCost;
        private final int expCost;
        private final double damageMultiplier;
        private final double expMultiplier;
        
        public Rank(String rankId, String name, int levelRequired, String colorCode,
                   int coinCost, int expCost, double damageMultiplier, double expMultiplier) {
            this.rankId = rankId;
            this.name = name;
            this.levelRequired = levelRequired;
            this.colorCode = colorCode;
            this.coinCost = coinCost;
            this.expCost = expCost;
            this.damageMultiplier = damageMultiplier;
            this.expMultiplier = expMultiplier;
        }
        
        // Getters
        public String getRankId() { return rankId; }
        public String getName() { return name; }
        public int getLevelRequired() { return levelRequired; }
        public String getColorCode() { return colorCode; }
        public int getCoinCost() { return coinCost; }
        public int getExpCost() { return expCost; }
        public double getDamageMultiplier() { return damageMultiplier; }
        public double getExpMultiplier() { return expMultiplier; }
    }
}
