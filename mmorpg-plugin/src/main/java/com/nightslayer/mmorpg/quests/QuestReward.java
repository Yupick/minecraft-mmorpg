package com.nightslayer.mmorpg.quests;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

/**
 * Represents rewards given upon quest completion
 */
public class QuestReward {
    
    private final int coins;
    private final int experience;
    private final List<ItemStack> items;
    private final Map<String, Integer> customRewards;
    private final String title;
    
    private QuestReward(Builder builder) {
        this.coins = builder.coins;
        this.experience = builder.experience;
        this.items = new ArrayList<>(builder.items);
        this.customRewards = new HashMap<>(builder.customRewards);
        this.title = builder.title;
    }
    
    /**
     * Gives all rewards to the player
     */
    public void giveRewards(Player player, com.nightslayer.mmorpg.economy.EconomyManager economyManager) {
        boolean hasRewards = false;
        
        // Send reward header
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("    ⭐ RECOMPENSAS DE QUEST ⭐    ", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        
        // Give coins
        if (coins > 0) {
            economyManager.deposit(player.getUniqueId(), coins);
            player.sendMessage(Component.text("  + ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text(coins + " coins", NamedTextColor.GOLD)));
            hasRewards = true;
        }
        
        // Give experience
        if (experience > 0) {
            player.giveExp(experience);
            player.sendMessage(Component.text("  + ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text(experience + " XP", NamedTextColor.AQUA)));
            hasRewards = true;
        }
        
        // Give items
        if (!items.isEmpty()) {
            for (ItemStack item : items) {
                player.getInventory().addItem(item.clone());
                String itemName = item.getType().name().replace("_", " ");
                player.sendMessage(Component.text("  + ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(item.getAmount() + "x ", NamedTextColor.WHITE))
                        .append(Component.text(itemName, NamedTextColor.YELLOW)));
            }
            hasRewards = true;
        }
        
        // Give title if present
        if (title != null && !title.isEmpty()) {
            player.sendMessage(Component.text("  + ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Título: ", NamedTextColor.GRAY))
                    .append(Component.text(title, NamedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC)));
            hasRewards = true;
        }
        
        // Custom rewards (for extension)
        if (!customRewards.isEmpty()) {
            for (Map.Entry<String, Integer> entry : customRewards.entrySet()) {
                player.sendMessage(Component.text("  + ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(entry.getValue() + " ", NamedTextColor.WHITE))
                        .append(Component.text(entry.getKey(), NamedTextColor.LIGHT_PURPLE)));
            }
            hasRewards = true;
        }
        
        if (!hasRewards) {
            player.sendMessage(Component.text("  Sin recompensas materiales", NamedTextColor.GRAY));
        }
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
    }
    
    /**
     * Checks if the player's inventory has space for all item rewards
     */
    public boolean hasInventorySpace(Player player) {
        if (items.isEmpty()) return true;
        
        int emptySlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        
        return emptySlots >= items.size();
    }
    
    /**
     * Gets a formatted summary of rewards
     */
    public List<String> getSummary() {
        List<String> summary = new ArrayList<>();
        
        if (coins > 0) {
            summary.add("§6" + coins + " coins");
        }
        if (experience > 0) {
            summary.add("§b" + experience + " XP");
        }
        if (!items.isEmpty()) {
            for (ItemStack item : items) {
                summary.add("§e" + item.getAmount() + "x " + item.getType().name().replace("_", " "));
            }
        }
        if (title != null) {
            summary.add("§dTítulo: " + title);
        }
        
        return summary;
    }
    
    // Getters
    public int getCoins() {
        return coins;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }
    
    public Map<String, Integer> getCustomRewards() {
        return new HashMap<>(customRewards);
    }
    
    public String getTitle() {
        return title;
    }
    
    /**
     * Builder for creating quest rewards
     */
    public static class Builder {
        private int coins = 0;
        private int experience = 0;
        private final List<ItemStack> items = new ArrayList<>();
        private final Map<String, Integer> customRewards = new HashMap<>();
        private String title = null;
        
        public Builder coins(int coins) {
            this.coins = coins;
            return this;
        }
        
        public Builder experience(int experience) {
            this.experience = experience;
            return this;
        }
        
        public Builder addItem(ItemStack item) {
            this.items.add(item);
            return this;
        }
        
        public Builder addItem(Material material, int amount) {
            this.items.add(new ItemStack(material, amount));
            return this;
        }
        
        public Builder addCustomReward(String rewardType, int amount) {
            this.customRewards.put(rewardType, amount);
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public QuestReward build() {
            return new QuestReward(this);
        }
    }
    
    /**
     * Creates a simple coin reward
     */
    public static QuestReward coins(int amount) {
        return new Builder().coins(amount).build();
    }
    
    /**
     * Creates a simple experience reward
     */
    public static QuestReward experience(int amount) {
        return new Builder().experience(amount).build();
    }
    
    /**
     * Creates a combined coin and experience reward
     */
    public static QuestReward coinsAndExp(int coins, int experience) {
        return new Builder()
                .coins(coins)
                .experience(experience)
                .build();
    }
    
    /**
     * Creates a reward with items
     */
    public static QuestReward items(ItemStack... items) {
        Builder builder = new Builder();
        for (ItemStack item : items) {
            builder.addItem(item);
        }
        return builder.build();
    }
}
