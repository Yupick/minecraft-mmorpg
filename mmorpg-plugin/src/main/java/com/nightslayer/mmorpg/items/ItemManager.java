package com.nightslayer.mmorpg.items;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private final MMORPGPlugin plugin;
    
    public ItemManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createCustomItem(String itemId, String name, List<String> lore, Material material) {
        ItemStack item = new ItemStack(material);
        ItemBuilder builder = new ItemBuilder(item, plugin)
            .setDisplayName(name)
            .setLore(lore)
            .setPersistentData("custom_item_id", itemId);
        return builder.build();
    }
    
    public boolean isCustomItem(ItemStack item, String itemId) {
        String id = ItemBuilder.getPersistentData(item, plugin, "custom_item_id");
        return id != null && id.equals(itemId);
    }
}
