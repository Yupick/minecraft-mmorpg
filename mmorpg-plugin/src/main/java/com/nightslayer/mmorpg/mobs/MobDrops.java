package com.nightslayer.mmorpg.mobs;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages custom drops for mobs
 */
public class MobDrops {
    
    private final List<Drop> drops;
    private final int minCoins;
    private final int maxCoins;
    private final int experienceMultiplier; // Percentage (100 = normal)
    
    public MobDrops() {
        this.drops = new ArrayList<>();
        this.minCoins = 0;
        this.maxCoins = 0;
        this.experienceMultiplier = 100;
    }
    
    public MobDrops(List<Drop> drops, int minCoins, int maxCoins, int experienceMultiplier) {
        this.drops = new ArrayList<>(drops);
        this.minCoins = minCoins;
        this.maxCoins = maxCoins;
        this.experienceMultiplier = experienceMultiplier;
    }
    
    /**
     * Rolls and generates drops based on probabilities
     */
    public List<ItemStack> generateDrops() {
        List<ItemStack> result = new ArrayList<>();
        Random random = new Random();
        
        for (Drop drop : drops) {
            // Roll for drop chance
            if (random.nextDouble() * 100 <= drop.getChance()) {
                int amount = random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1) + drop.getMinAmount();
                if (amount > 0) {
                    ItemStack item = new ItemStack(drop.getMaterial(), amount);
                    result.add(item);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets the coins to drop (random between min and max)
     */
    public int getCoinsDropped() {
        if (minCoins >= maxCoins) return minCoins;
        Random random = new Random();
        return random.nextInt(maxCoins - minCoins + 1) + minCoins;
    }
    
    /**
     * Calculates modified experience based on multiplier
     */
    public int getModifiedExperience(int baseExperience) {
        return (int) (baseExperience * (experienceMultiplier / 100.0));
    }
    
    /**
     * Adds a drop to the list
     */
    public void addDrop(Drop drop) {
        this.drops.add(drop);
    }
    
    /**
     * Removes all drops of a specific material
     */
    public void removeDrop(Material material) {
        drops.removeIf(drop -> drop.getMaterial() == material);
    }
    
    // Getters
    public List<Drop> getDrops() {
        return new ArrayList<>(drops);
    }
    
    public int getMinCoins() {
        return minCoins;
    }
    
    public int getMaxCoins() {
        return maxCoins;
    }
    
    public int getExperienceMultiplier() {
        return experienceMultiplier;
    }
    
    /**
     * Represents a single drop item with chance
     */
    public static class Drop {
        private final Material material;
        private final int minAmount;
        private final int maxAmount;
        private final double chance; // Percentage (0-100)
        
        public Drop(Material material, int minAmount, int maxAmount, double chance) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
        }
        
        public Drop(Material material, int amount, double chance) {
            this(material, amount, amount, chance);
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public int getMinAmount() {
            return minAmount;
        }
        
        public int getMaxAmount() {
            return maxAmount;
        }
        
        public double getChance() {
            return chance;
        }
        
        @Override
        public String toString() {
            if (minAmount == maxAmount) {
                return String.format("%dx %s (%.1f%%)", minAmount, material.name(), chance);
            } else {
                return String.format("%d-%dx %s (%.1f%%)", minAmount, maxAmount, material.name(), chance);
            }
        }
    }
    
    /**
     * Builder for creating mob drops
     */
    public static class Builder {
        private final List<Drop> drops = new ArrayList<>();
        private int minCoins = 0;
        private int maxCoins = 0;
        private int experienceMultiplier = 100;
        
        public Builder addDrop(Material material, int amount, double chance) {
            drops.add(new Drop(material, amount, chance));
            return this;
        }
        
        public Builder addDrop(Material material, int minAmount, int maxAmount, double chance) {
            drops.add(new Drop(material, minAmount, maxAmount, chance));
            return this;
        }
        
        public Builder addDrop(Drop drop) {
            drops.add(drop);
            return this;
        }
        
        public Builder coins(int minCoins, int maxCoins) {
            this.minCoins = minCoins;
            this.maxCoins = maxCoins;
            return this;
        }
        
        public Builder coins(int exactCoins) {
            this.minCoins = exactCoins;
            this.maxCoins = exactCoins;
            return this;
        }
        
        public Builder experienceMultiplier(int multiplier) {
            this.experienceMultiplier = multiplier;
            return this;
        }
        
        public MobDrops build() {
            return new MobDrops(drops, minCoins, maxCoins, experienceMultiplier);
        }
    }
    
    /**
     * Creates a simple drop configuration
     */
    public static MobDrops simple(Material material, int amount, double chance, int coins) {
        return new Builder()
                .addDrop(material, amount, chance)
                .coins(coins)
                .build();
    }
    
    /**
     * Creates a common mob drop (bones, rotten flesh, etc.)
     */
    public static MobDrops common() {
        return new Builder()
                .addDrop(Material.BONE, 0, 2, 50.0)
                .addDrop(Material.ROTTEN_FLESH, 0, 1, 30.0)
                .coins(1, 5)
                .build();
    }
    
    /**
     * Creates a rare mob drop (diamonds, emeralds, etc.)
     */
    public static MobDrops rare() {
        return new Builder()
                .addDrop(Material.DIAMOND, 1, 2, 5.0)
                .addDrop(Material.EMERALD, 1, 3, 10.0)
                .addDrop(Material.GOLD_INGOT, 2, 5, 25.0)
                .coins(50, 100)
                .experienceMultiplier(200)
                .build();
    }
    
    /**
     * Creates a boss mob drop (enchanted items, rare materials)
     */
    public static MobDrops boss() {
        return new Builder()
                .addDrop(Material.DIAMOND, 3, 5, 50.0)
                .addDrop(Material.NETHERITE_SCRAP, 1, 2, 25.0)
                .addDrop(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 100.0)
                .addDrop(Material.NETHER_STAR, 1, 1, 15.0)
                .coins(500, 1000)
                .experienceMultiplier(500)
                .build();
    }
}
