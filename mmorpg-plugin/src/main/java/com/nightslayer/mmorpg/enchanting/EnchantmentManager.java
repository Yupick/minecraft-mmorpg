package com.nightslayer.mmorpg.enchanting;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages custom enchantments and enchanting operations
 * 
 * Features:
 * - Load enchantments from database
 * - Apply enchantments to items
 * - Validate level requirements
 * - Cost system (coins + exp)
 */
public class EnchantmentManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<String, RPGEnchantment> enchantments;
    
    public EnchantmentManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.enchantments = new HashMap<>();
    }
    
    /**
     * Load all enchantments from database
     */
    public void loadEnchantments() {
        enchantments.clear();
        String sql = "SELECT * FROM enchantments";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                RPGEnchantment ench = new RPGEnchantment(
                    rs.getString("enchantment_id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getInt("max_level"),
                    rs.getInt("level_required"),
                    rs.getInt("coin_cost_per_level"),
                    rs.getInt("exp_cost_per_level"),
                    rs.getString("applicable_items")
                );
                enchantments.put(ench.getEnchantmentId(), ench);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + enchantments.size() + " custom enchantments");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading enchantments", e);
        }
    }
    
    /**
     * Get an enchantment by ID
     */
    public RPGEnchantment getEnchantment(String enchantmentId) {
        return enchantments.get(enchantmentId);
    }
    
    /**
     * Get all enchantments available for a player
     */
    public List<RPGEnchantment> getAvailableEnchantments(Player player) {
        List<RPGEnchantment> available = new ArrayList<>();
        int playerLevel = getPlayerLevel(player);
        
        for (RPGEnchantment ench : enchantments.values()) {
            if (ench.getLevelRequired() <= playerLevel) {
                available.add(ench);
            }
        }
        
        return available;
    }
    
    /**
     * Apply enchantment to an item
     */
    public boolean applyEnchantment(Player player, ItemStack item, String enchantmentId, int level) {
        RPGEnchantment rpgEnch = enchantments.get(enchantmentId);
        if (rpgEnch == null) {
            player.sendMessage(langManager.getMessage("enchanting.not_found"));
            return false;
        }
        
        // Check player level
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < rpgEnch.getLevelRequired()) {
            player.sendMessage(langManager.getMessage("enchanting.level_too_low", 
                rpgEnch.getLevelRequired()));
            return false;
        }
        
        // Check enchantment level
        if (level < 1 || level > rpgEnch.getMaxLevel()) {
            player.sendMessage(langManager.getMessage("enchanting.invalid_level", 
                rpgEnch.getMaxLevel()));
            return false;
        }
        
        // Check if item is applicable
        if (!rpgEnch.isApplicable(item)) {
            player.sendMessage(langManager.getMessage("enchanting.not_applicable"));
            return false;
        }
        
        // Calculate costs
        int coinCost = rpgEnch.getCoinCostPerLevel() * level;
        int expCost = rpgEnch.getExpCostPerLevel() * level;
        
        // Check player balance
        int playerBalance = getPlayerBalance(player);
        if (playerBalance < coinCost) {
            player.sendMessage(langManager.getMessage("enchanting.insufficient_coins"));
            return false;
        }
        
        // Check player exp
        int playerExp = getPlayerExp(player);
        if (playerExp < expCost) {
            player.sendMessage(langManager.getMessage("enchanting.insufficient_exp"));
            return false;
        }
        
        // Apply vanilla enchantment
        Enchantment vanillaEnch = getVanillaEnchantment(rpgEnch.getType());
        if (vanillaEnch != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(vanillaEnch, level, true);
                item.setItemMeta(meta);
            }
        }
        
        // Charge costs
        chargeCoins(player, coinCost);
        chargeExp(player, expCost);
        
        player.sendMessage(langManager.getMessage("enchanting.success", 
            rpgEnch.getName(), level));
        
        return true;
    }
    
    /**
     * Map RPG enchantment type to vanilla enchantment
     */
    private Enchantment getVanillaEnchantment(String type) {
        return switch (type.toUpperCase()) {
            case "SHARPNESS" -> Enchantment.SHARPNESS;
            case "PROTECTION" -> Enchantment.PROTECTION;
            case "UNBREAKING" -> Enchantment.UNBREAKING;
            case "EFFICIENCY" -> Enchantment.EFFICIENCY;
            case "FORTUNE" -> Enchantment.FORTUNE;
            case "POWER" -> Enchantment.POWER;
            case "FLAME" -> Enchantment.FLAME;
            case "INFINITY" -> Enchantment.INFINITY;
            case "LOOTING" -> Enchantment.LOOTING;
            case "SILK_TOUCH" -> Enchantment.SILK_TOUCH;
            case "THORNS" -> Enchantment.THORNS;
            case "MENDING" -> Enchantment.MENDING;
            default -> null;
        };
    }
    
    /**
     * Get player level from database
     */
    private int getPlayerLevel(Player player) {
        String sql = "SELECT level FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player level", e);
        }
        
        return 1;
    }
    
    /**
     * Get player balance from database
     */
    private int getPlayerBalance(Player player) {
        String sql = "SELECT coins FROM player_economy WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("coins");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player balance", e);
        }
        
        return 0;
    }
    
    /**
     * Get player exp from database
     */
    private int getPlayerExp(Player player) {
        String sql = "SELECT experience FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("experience");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player exp", e);
        }
        
        return 0;
    }
    
    /**
     * Charge coins from player
     */
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
    
    /**
     * Charge exp from player
     */
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
     * Inner class representing a custom enchantment
     */
    public static class RPGEnchantment {
        private final String enchantmentId;
        private final String name;
        private final String type;
        private final int maxLevel;
        private final int levelRequired;
        private final int coinCostPerLevel;
        private final int expCostPerLevel;
        private final String applicableItems;
        
        public RPGEnchantment(String enchantmentId, String name, String type, int maxLevel,
                             int levelRequired, int coinCostPerLevel, int expCostPerLevel,
                             String applicableItems) {
            this.enchantmentId = enchantmentId;
            this.name = name;
            this.type = type;
            this.maxLevel = maxLevel;
            this.levelRequired = levelRequired;
            this.coinCostPerLevel = coinCostPerLevel;
            this.expCostPerLevel = expCostPerLevel;
            this.applicableItems = applicableItems;
        }
        
        /**
         * Check if enchantment can be applied to item
         */
        public boolean isApplicable(ItemStack item) {
            String itemType = item.getType().name();
            String[] applicable = applicableItems.split(",");
            
            for (String type : applicable) {
                if (itemType.contains(type.trim().toUpperCase())) {
                    return true;
                }
            }
            
            return false;
        }
        
        // Getters
        public String getEnchantmentId() { return enchantmentId; }
        public String getName() { return name; }
        public String getType() { return type; }
        public int getMaxLevel() { return maxLevel; }
        public int getLevelRequired() { return levelRequired; }
        public int getCoinCostPerLevel() { return coinCostPerLevel; }
        public int getExpCostPerLevel() { return expCostPerLevel; }
        public String getApplicableItems() { return applicableItems; }
    }
}
