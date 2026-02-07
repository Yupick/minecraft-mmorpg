package com.nightslayer.mmorpg.crafting;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages custom crafting recipes and crafting operations
 * 
 * Features:
 * - Load recipes from database
 * - Validate materials
 * - Process crafting with costs
 * - Level requirements
 */
public class CraftingManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<String, CraftingRecipe> recipes;
    
    public CraftingManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.recipes = new HashMap<>();
    }
    
    /**
     * Load all crafting recipes from database
     */
    public void loadRecipes() {
        recipes.clear();
        String sql = "SELECT * FROM crafting_recipes";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                CraftingRecipe recipe = new CraftingRecipe(
                    rs.getString("recipe_id"),
                    rs.getString("name"),
                    rs.getString("result_item"),
                    rs.getInt("result_amount"),
                    rs.getString("materials"),
                    rs.getInt("level_required"),
                    rs.getInt("coin_cost"),
                    rs.getInt("exp_cost"),
                    rs.getInt("craft_exp_reward")
                );
                recipes.put(recipe.getRecipeId(), recipe);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + recipes.size() + " crafting recipes");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading crafting recipes", e);
        }
    }
    
    /**
     * Get a recipe by ID
     */
    public CraftingRecipe getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }
    
    /**
     * Get all available recipes for a player (based on level)
     */
    public List<CraftingRecipe> getAvailableRecipes(Player player) {
        List<CraftingRecipe> available = new ArrayList<>();
        int playerLevel = getPlayerLevel(player);
        
        for (CraftingRecipe recipe : recipes.values()) {
            if (recipe.getLevelRequired() <= playerLevel) {
                available.add(recipe);
            }
        }
        
        return available;
    }
    
    /**
     * Attempt to craft an item
     */
    public boolean craftItem(Player player, String recipeId) {
        CraftingRecipe recipe = recipes.get(recipeId);
        if (recipe == null) {
            player.sendMessage(langManager.getMessage("crafting.recipe_not_found"));
            return false;
        }
        
        // Check level requirement
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < recipe.getLevelRequired()) {
            player.sendMessage(langManager.getMessage("crafting.level_too_low", 
                recipe.getLevelRequired()));
            return false;
        }
        
        // Check if player has materials
        if (!hasMaterials(player, recipe)) {
            player.sendMessage(langManager.getMessage("crafting.missing_materials"));
            return false;
        }
        
        // Check if player has enough coins
        int playerBalance = getPlayerBalance(player);
        if (playerBalance < recipe.getCoinCost()) {
            player.sendMessage(langManager.getMessage("crafting.insufficient_coins"));
            return false;
        }
        
        // Remove materials
        removeMaterials(player, recipe);
        
        // Charge coin cost
        if (recipe.getCoinCost() > 0) {
            chargeCoins(player, recipe.getCoinCost());
        }
        
        // Give result item
        Material resultMaterial = Material.getMaterial(recipe.getResultItem().toUpperCase());
        if (resultMaterial != null) {
            ItemStack result = new ItemStack(resultMaterial, recipe.getResultAmount());
            player.getInventory().addItem(result);
        }
        
        // Give exp reward
        if (recipe.getCraftExpReward() > 0) {
            giveExp(player, recipe.getCraftExpReward());
        }
        
        player.sendMessage(langManager.getMessage("crafting.success", 
            recipe.getName(), recipe.getResultAmount()));
        
        return true;
    }
    
    /**
     * Check if player has required materials
     */
    private boolean hasMaterials(Player player, CraftingRecipe recipe) {
        Map<Material, Integer> required = recipe.getParsedMaterials();
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            
            if (!hasItem(player, material, amount)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if player has specific item amount
     */
    private boolean hasItem(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }
    
    /**
     * Remove materials from player inventory
     */
    private void removeMaterials(Player player, CraftingRecipe recipe) {
        Map<Material, Integer> required = recipe.getParsedMaterials();
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            removeItem(player, material, amount);
        }
    }
    
    /**
     * Remove specific amount of item from player
     */
    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                
                if (remaining == 0) break;
            }
        }
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
     * Give exp to player
     */
    private void giveExp(Player player, int amount) {
        String sql = "UPDATE players SET experience = experience + ? WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error giving exp", e);
        }
    }
    
    /**
     * Inner class representing a crafting recipe
     */
    public static class CraftingRecipe {
        private final String recipeId;
        private final String name;
        private final String resultItem;
        private final int resultAmount;
        private final String materials;
        private final int levelRequired;
        private final int coinCost;
        private final int expCost;
        private final int craftExpReward;
        
        public CraftingRecipe(String recipeId, String name, String resultItem, int resultAmount,
                             String materials, int levelRequired, int coinCost, int expCost,
                             int craftExpReward) {
            this.recipeId = recipeId;
            this.name = name;
            this.resultItem = resultItem;
            this.resultAmount = resultAmount;
            this.materials = materials;
            this.levelRequired = levelRequired;
            this.coinCost = coinCost;
            this.expCost = expCost;
            this.craftExpReward = craftExpReward;
        }
        
        /**
         * Parse materials string into Map
         * Format: "DIAMOND:2,STICK:3"
         */
        public Map<Material, Integer> getParsedMaterials() {
            Map<Material, Integer> parsed = new HashMap<>();
            String[] parts = materials.split(",");
            
            for (String part : parts) {
                String[] itemParts = part.trim().split(":");
                if (itemParts.length == 2) {
                    Material material = Material.getMaterial(itemParts[0].toUpperCase());
                    int amount = Integer.parseInt(itemParts[1]);
                    
                    if (material != null) {
                        parsed.put(material, amount);
                    }
                }
            }
            
            return parsed;
        }
        
        // Getters
        public String getRecipeId() { return recipeId; }
        public String getName() { return name; }
        public String getResultItem() { return resultItem; }
        public int getResultAmount() { return resultAmount; }
        public String getMaterials() { return materials; }
        public int getLevelRequired() { return levelRequired; }
        public int getCoinCost() { return coinCost; }
        public int getExpCost() { return expCost; }
        public int getCraftExpReward() { return craftExpReward; }
    }
}
