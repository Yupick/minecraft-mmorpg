package com.nightslayer.mmorpg.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import com.nightslayer.mmorpg.MMORPGPlugin;

import java.util.*;

/**
 * Represents a custom RPG item with stats and abilities
 */
public class RPGItem {
    
    private final String itemId;
    private final String name;
    private final Material material;
    private final ItemRarity rarity;
    private final int requiredLevel;
    private final ItemStats stats;
    private final List<String> abilities;
    private final String lore;
    
    private RPGItem(Builder builder) {
        this.itemId = builder.itemId;
        this.name = builder.name;
        this.material = builder.material;
        this.rarity = builder.rarity;
        this.requiredLevel = builder.requiredLevel;
        this.stats = builder.stats;
        this.abilities = new ArrayList<>(builder.abilities);
        this.lore = builder.lore;
    }
    
    /**
     * Creates an ItemStack from this RPG item
     */
    public ItemStack createItemStack(MMORPGPlugin plugin) {
        ItemStack item = new ItemStack(material);
        
        // Set display name with rarity color
        net.kyori.adventure.text.Component displayName = net.kyori.adventure.text.Component.text(name)
                .color(rarity.getColor())
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
        
        item.editMeta(meta -> {
            meta.displayName(displayName);
            
            // Build lore
            List<net.kyori.adventure.text.Component> loreLines = new ArrayList<>();
            loreLines.add(net.kyori.adventure.text.Component.text(""));
            loreLines.add(net.kyori.adventure.text.Component.text(rarity.getDisplayName(), rarity.getColor()));
            
            if (requiredLevel > 1) {
                loreLines.add(net.kyori.adventure.text.Component.text("Nivel requerido: " + requiredLevel)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
            
            // Add stats to lore
            if (stats.hasStats()) {
                loreLines.add(net.kyori.adventure.text.Component.text(""));
                loreLines.addAll(stats.toLore());
            }
            
            // Add abilities to lore
            if (!abilities.isEmpty()) {
                loreLines.add(net.kyori.adventure.text.Component.text(""));
                loreLines.add(net.kyori.adventure.text.Component.text("Habilidades:")
                        .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                for (String ability : abilities) {
                    loreLines.add(net.kyori.adventure.text.Component.text("  • " + ability)
                            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                }
            }
            
            // Add custom lore
            if (lore != null && !lore.isEmpty()) {
                loreLines.add(net.kyori.adventure.text.Component.text(""));
                loreLines.add(net.kyori.adventure.text.Component.text(lore)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC));
            }
            
            meta.lore(loreLines);
            
            // Store RPG data in persistent data
            NamespacedKey itemIdKey = new NamespacedKey(plugin, "rpg_item_id");
            NamespacedKey rarityKey = new NamespacedKey(plugin, "rpg_rarity");
            NamespacedKey levelKey = new NamespacedKey(plugin, "rpg_required_level");
            
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, itemId);
            meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, rarity.name());
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, requiredLevel);
            
            // Store stats
            stats.saveToPersistentData(meta.getPersistentDataContainer(), plugin);
        });
        
        return item;
    }
    
    /**
     * Checks if an ItemStack is an RPG item
     */
    public static boolean isRPGItem(ItemStack item, MMORPGPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return false;
        
        NamespacedKey itemIdKey = new NamespacedKey(plugin, "rpg_item_id");
        return item.getItemMeta().getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the RPG item ID from an ItemStack
     */
    public static String getItemId(ItemStack item, MMORPGPlugin plugin) {
        if (!isRPGItem(item, plugin)) return null;
        
        NamespacedKey itemIdKey = new NamespacedKey(plugin, "rpg_item_id");
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the rarity of an RPG item
     */
    public static ItemRarity getRarity(ItemStack item, MMORPGPlugin plugin) {
        if (!isRPGItem(item, plugin)) return ItemRarity.COMMON;
        
        NamespacedKey rarityKey = new NamespacedKey(plugin, "rpg_rarity");
        String rarityStr = item.getItemMeta().getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
        
        try {
            return ItemRarity.valueOf(rarityStr);
        } catch (Exception e) {
            return ItemRarity.COMMON;
        }
    }
    
    /**
     * Gets the required level of an RPG item
     */
    public static int getRequiredLevel(ItemStack item, MMORPGPlugin plugin) {
        if (!isRPGItem(item, plugin)) return 1;
        
        NamespacedKey levelKey = new NamespacedKey(plugin, "rpg_required_level");
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level != null ? level : 1;
    }
    
    // Getters
    public String getItemId() {
        return itemId;
    }
    
    public String getName() {
        return name;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public ItemRarity getRarity() {
        return rarity;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public ItemStats getStats() {
        return stats;
    }
    
    public List<String> getAbilities() {
        return new ArrayList<>(abilities);
    }
    
    public String getLore() {
        return lore;
    }
    
    /**
     * Item rarity tiers
     */
    public enum ItemRarity {
        COMMON("Común", net.kyori.adventure.text.format.NamedTextColor.WHITE, 1.0),
        UNCOMMON("Poco Común", net.kyori.adventure.text.format.NamedTextColor.GREEN, 1.2),
        RARE("Raro", net.kyori.adventure.text.format.NamedTextColor.BLUE, 1.5),
        EPIC("Épico", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE, 2.0),
        LEGENDARY("Legendario", net.kyori.adventure.text.format.NamedTextColor.GOLD, 3.0),
        MYTHIC("Mítico", net.kyori.adventure.text.format.NamedTextColor.RED, 5.0);
        
        private final String displayName;
        private final net.kyori.adventure.text.format.TextColor color;
        private final double statMultiplier;
        
        ItemRarity(String displayName, net.kyori.adventure.text.format.TextColor color, double statMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.statMultiplier = statMultiplier;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public net.kyori.adventure.text.format.TextColor getColor() {
            return color;
        }
        
        public double getStatMultiplier() {
            return statMultiplier;
        }
    }
    
    /**
     * Builder for creating RPG items
     */
    public static class Builder {
        private String itemId;
        private String name;
        private Material material = Material.STICK;
        private ItemRarity rarity = ItemRarity.COMMON;
        private int requiredLevel = 1;
        private ItemStats stats = new ItemStats();
        private final List<String> abilities = new ArrayList<>();
        private String lore = "";
        
        public Builder(String itemId, String name) {
            this.itemId = itemId;
            this.name = name;
        }
        
        public Builder material(Material material) {
            this.material = material;
            return this;
        }
        
        public Builder rarity(ItemRarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        public Builder requiredLevel(int level) {
            this.requiredLevel = level;
            return this;
        }
        
        public Builder stats(ItemStats stats) {
            this.stats = stats;
            return this;
        }
        
        public Builder addAbility(String ability) {
            this.abilities.add(ability);
            return this;
        }
        
        public Builder lore(String lore) {
            this.lore = lore;
            return this;
        }
        
        public RPGItem build() {
            return new RPGItem(this);
        }
    }
}
