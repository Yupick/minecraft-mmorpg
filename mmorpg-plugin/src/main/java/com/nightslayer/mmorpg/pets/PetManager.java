package com.nightslayer.mmorpg.pets;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages pets system
 * 
 * Features:
 * - Load pets from database
 * - Adopt/abandon pets
 * - Pet training and leveling
 * - Mount functionality
 * - Pet abilities
 */
public class PetManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<String, PetDefinition> petDefinitions;
    private final Map<UUID, LivingEntity> activePets; // player UUID -> pet entity
    
    public PetManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.petDefinitions = new HashMap<>();
        this.activePets = new HashMap<>();
    }
    
    /**
     * Load all pet definitions from database
     */
    public void loadPets() {
        petDefinitions.clear();
        String sql = "SELECT * FROM pets";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                PetDefinition pet = new PetDefinition(
                    rs.getString("pet_id"),
                    rs.getString("name"),
                    rs.getString("entity_type"),
                    rs.getInt("unlock_level"),
                    rs.getInt("coin_cost"),
                    rs.getInt("max_level"),
                    rs.getDouble("base_health"),
                    rs.getDouble("base_damage"),
                    rs.getBoolean("can_mount"),
                    rs.getString("abilities")
                );
                petDefinitions.put(pet.getPetId(), pet);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + petDefinitions.size() + " pet definitions");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading pets", e);
        }
    }
    
    /**
     * Get a pet definition by ID
     */
    public PetDefinition getPetDefinition(String petId) {
        return petDefinitions.get(petId);
    }
    
    /**
     * Adopt a pet
     */
    public boolean adoptPet(Player player, String petId) {
        PetDefinition petDef = petDefinitions.get(petId);
        if (petDef == null) {
            player.sendMessage(langManager.getMessage("pet.not_found"));
            return false;
        }
        
        // Check player level
        int playerLevel = getPlayerLevel(player);
        if (playerLevel < petDef.getUnlockLevel()) {
            player.sendMessage(langManager.getMessage("pet.level_too_low", petDef.getUnlockLevel()));
            return false;
        }
        
        // Check if player already owns this pet
        if (hasPlayerPet(player, petId)) {
            player.sendMessage(langManager.getMessage("pet.already_owned"));
            return false;
        }
        
        // Check balance
        int balance = getPlayerBalance(player);
        if (balance < petDef.getCoinCost()) {
            player.sendMessage(langManager.getMessage("pet.insufficient_coins"));
            return false;
        }
        
        // Charge cost
        chargeCoins(player, petDef.getCoinCost());
        
        // Add pet to player
        String sql = "INSERT INTO player_pets (uuid, pet_id, name, level, experience) VALUES (?, ?, ?, 1, 0)";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, petId);
            stmt.setString(3, petDef.getName());
            stmt.executeUpdate();
            
            player.sendMessage(langManager.getMessage("pet.adopted", petDef.getName()));
            return true;
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error adopting pet", e);
            return false;
        }
    }
    
    /**
     * Abandon a pet
     */
    public boolean abandonPet(Player player, String petId) {
        // Remove active pet if summoned
        despawnPet(player);
        
        // Remove from database
        String sql = "DELETE FROM player_pets WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, petId);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                player.sendMessage(langManager.getMessage("pet.abandoned"));
                return true;
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error abandoning pet", e);
        }
        
        return false;
    }
    
    /**
     * Spawn/summon a pet
     */
    public boolean spawnPet(Player player, String petId) {
        // Despawn current pet if any
        despawnPet(player);
        
        PetDefinition petDef = petDefinitions.get(petId);
        if (petDef == null) {
            return false;
        }
        
        // Check if player owns this pet
        if (!hasPlayerPet(player, petId)) {
            player.sendMessage(langManager.getMessage("pet.not_owned"));
            return false;
        }
        
        // Get pet level
        int petLevel = getPlayerPetLevel(player, petId);
        
        // Spawn entity
        EntityType type = EntityType.valueOf(petDef.getEntityType().toUpperCase());
        LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(
            player.getLocation(),
            type
        );
        
        // Configure entity
        entity.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize("§e" + petDef.getName() + " §7(Lv." + petLevel + ")"));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        
        // Apply stats based on level
        double health = petDef.getBaseHealth() * (1 + petLevel * 0.1);
        if (entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            entity.setHealth(health);
        }
        
        // Make tameable if applicable
        if (entity instanceof Tameable tameable) {
            tameable.setOwner(player);
            tameable.setTamed(true);
        }
        
        activePets.put(player.getUniqueId(), entity);
        player.sendMessage(langManager.getMessage("pet.summoned", petDef.getName()));
        
        return true;
    }
    
    /**
     * Despawn a pet
     */
    public void despawnPet(Player player) {
        LivingEntity pet = activePets.remove(player.getUniqueId());
        if (pet != null && !pet.isDead()) {
            pet.remove();
        }
    }
    
    /**
     * Get player's active pet
     */
    public LivingEntity getActivePet(Player player) {
        return activePets.get(player.getUniqueId());
    }
    
    /**
     * Train pet (give exp)
     */
    public void trainPet(Player player, String petId, int exp) {
        String sql = "UPDATE player_pets SET experience = experience + ? WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, exp);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.setString(3, petId);
            stmt.executeUpdate();
            
            // Check for level up
            checkPetLevelUp(player, petId);
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error training pet", e);
        }
    }
    
    /**
     * Check if pet should level up
     */
    private void checkPetLevelUp(Player player, String petId) {
        String sql = "SELECT level, experience FROM player_pets WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, petId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int level = rs.getInt("level");
                    int exp = rs.getInt("experience");
                    int requiredExp = level * 100;
                    
                    if (exp >= requiredExp) {
                        levelUpPet(player, petId, level + 1);
                    }
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error checking pet level up", e);
        }
    }
    
    /**
     * Level up a pet
     */
    private void levelUpPet(Player player, String petId, int newLevel) {
        String sql = "UPDATE player_pets SET level = ?, experience = 0 WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newLevel);
            stmt.setString(2, player.getUniqueId().toString());
            stmt.setString(3, petId);
            stmt.executeUpdate();
            
            PetDefinition petDef = petDefinitions.get(petId);
            if (petDef != null) {
                player.sendMessage(langManager.getMessage("pet.level_up", 
                    petDef.getName(), newLevel));
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error leveling up pet", e);
        }
    }
    
    /**
     * Check if player owns a pet
     */
    private boolean hasPlayerPet(Player player, String petId) {
        String sql = "SELECT 1 FROM player_pets WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, petId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error checking player pet", e);
        }
        
        return false;
    }
    
    /**
     * Get player pet level
     */
    private int getPlayerPetLevel(Player player, String petId) {
        String sql = "SELECT level FROM player_pets WHERE uuid = ? AND pet_id = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, petId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting pet level", e);
        }
        
        return 1;
    }
    
    // Helper methods (similar to other managers)
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
     * Inner class representing a pet definition
     */
    public static class PetDefinition {
        private final String petId;
        private final String name;
        private final String entityType;
        private final int unlockLevel;
        private final int coinCost;
        private final int maxLevel;
        private final double baseHealth;
        private final double baseDamage;
        private final boolean canMount;
        private final String abilities;
        
        public PetDefinition(String petId, String name, String entityType, int unlockLevel, int coinCost,
                            int maxLevel, double baseHealth, double baseDamage, boolean canMount, String abilities) {
            this.petId = petId;
            this.name = name;
            this.entityType = entityType;
            this.unlockLevel = unlockLevel;
            this.coinCost = coinCost;
            this.maxLevel = maxLevel;
            this.baseHealth = baseHealth;
            this.baseDamage = baseDamage;
            this.canMount = canMount;
            this.abilities = abilities;
        }
        
        // Getters
        public String getPetId() { return petId; }
        public String getName() { return name; }
        public String getEntityType() { return entityType; }
        public int getUnlockLevel() { return unlockLevel; }
        public int getCoinCost() { return coinCost; }
        public int getMaxLevel() { return maxLevel; }
        public double getBaseHealth() { return baseHealth; }
        public double getBaseDamage() { return baseDamage; }
        public boolean canMount() { return canMount; }
        public String getAbilities() { return abilities; }
    }
}
