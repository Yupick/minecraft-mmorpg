package com.nightslayer.mmorpg.utils;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating and managing custom items.
 */
public class ItemBuilder {
    
    private final ItemStack item;
    private final MMORPGPlugin plugin;
    
    public ItemBuilder(ItemStack item, MMORPGPlugin plugin) {
        this.item = item;
        this.plugin = plugin;
    }
    
    /**
     * Set the display name of the item.
     */
    public ItemBuilder setDisplayName(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.replace('&', '§'));
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Set the lore of the item.
     */
    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(line.replace('&', '§'));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Add a line to the lore.
     */
    public ItemBuilder addLoreLine(String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(line.replace('&', '§'));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Add an enchantment to the item.
     */
    public ItemBuilder addEnchantment(Enchantment enchant, int level) {
        item.addUnsafeEnchantment(enchant, level);
        return this;
    }
    
    /**
     * Set persistent data to the item.
     */
    public ItemBuilder setPersistentData(String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Set persistent integer data to the item.
     */
    public ItemBuilder setPersistentData(String key, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, value);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Get persistent string data from an item.
     */
    public static String getPersistentData(ItemStack item, MMORPGPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }
    
    /**
     * Get persistent integer data from an item.
     */
    public static Integer getPersistentDataInt(ItemStack item, MMORPGPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Set item as unbreakable.
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    /**
     * Build and return the item.
     */
    public ItemStack build() {
        return item;
    }
}
