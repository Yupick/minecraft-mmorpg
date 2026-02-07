package com.nightslayer.mmorpg.npcs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.economy.EconomyManager;
import com.nightslayer.mmorpg.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages NPC trading with custom shops and item exchange
 */
public class NPCTrade {
    
    private final String tradeId;
    private final String npcName;
    private final List<TradeOffer> offers;
    private final EconomyManager economyManager;
    private final MMORPGPlugin plugin;
    
    public NPCTrade(String tradeId, String npcName, EconomyManager economyManager, MMORPGPlugin plugin) {
        this.tradeId = tradeId;
        this.npcName = npcName;
        this.offers = new ArrayList<>();
        this.economyManager = economyManager;
        this.plugin = plugin;
    }
    
    /**
     * Adds a trade offer to this NPC
     */
    public void addOffer(TradeOffer offer) {
        this.offers.add(offer);
    }
    
    /**
     * Opens the trade GUI for a player
     */
    public void openTradeGUI(Player player) {
        if (offers.isEmpty()) {
            player.sendMessage(Component.text("Este NPC no tiene nada para vender.", NamedTextColor.RED));
            return;
        }
        
        // Create inventory with size based on offers (multiple of 9)
        int size = Math.min(54, ((offers.size() / 9) + 1) * 9);
        Inventory tradeInv = Bukkit.createInventory(null, size, 
                Component.text("Tienda de " + npcName, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        
        // Populate with offers
        int slot = 0;
        for (TradeOffer offer : offers) {
            if (slot >= size) break;
            
            ItemStack displayItem = offer.getDisplayItem().clone();
            
            // Add lore with price and stock
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text("Precio: ", NamedTextColor.GRAY)
                    .append(Component.text(offer.getPrice() + " coins", NamedTextColor.GOLD)));
            
            if (offer.getStock() > 0) {
                lore.add(Component.text("Stock: ", NamedTextColor.GRAY)
                        .append(Component.text(offer.getStock(), NamedTextColor.GREEN)));
            } else if (offer.getStock() == 0) {
                lore.add(Component.text("¡AGOTADO!", NamedTextColor.RED, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Stock: ", NamedTextColor.GRAY)
                        .append(Component.text("Ilimitado", NamedTextColor.AQUA)));
            }
            
                lore.add(Component.text(""));
                lore.add(Component.text("Click izquierdo para comprar", NamedTextColor.YELLOW));
            
            // Build item with lore
                List<String> legacyLore = new ArrayList<>();
                for (Component line : lore) {
                legacyLore.add(PlainTextComponentSerializer.plainText().serialize(line));
                }
                ItemStack finalItem = new ItemBuilder(displayItem, plugin)
                    .setLore(legacyLore)
                    .build();
            
            tradeInv.setItem(slot, finalItem);
            slot++;
        }
        
        player.openInventory(tradeInv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }
    
    /**
     * Processes a purchase
     */
    public boolean purchase(Player player, int offerIndex) {
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return false;
        }
        
        TradeOffer offer = offers.get(offerIndex);
        
        // Check stock
        if (offer.getStock() == 0) {
            player.sendMessage(Component.text("Este artículo está agotado.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        
        // Check balance
        int balance = economyManager.getBalance(player.getUniqueId());
        if (balance < offer.getPrice()) {
            player.sendMessage(Component.text("No tienes suficientes coins. ", NamedTextColor.RED)
                    .append(Component.text("Necesitas: " + offer.getPrice() + ", tienes: " + balance, NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        
        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Tu inventario está lleno.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        
        // Process purchase
        economyManager.withdraw(player.getUniqueId(), offer.getPrice());
        player.getInventory().addItem(offer.getItem().clone());
        
        // Update stock
        if (offer.getStock() > 0) {
            offer.decreaseStock();
        }
        
        // Send success message
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("Compraste ", NamedTextColor.GREEN))
                .append(Component.text(offer.getItem().getType().name().replace("_", " "), NamedTextColor.YELLOW))
                .append(Component.text(" por ", NamedTextColor.GREEN))
                .append(Component.text(offer.getPrice() + " coins", NamedTextColor.GOLD)));
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        return true;
    }
    
    /**
     * Sells an item to the NPC (player sells to NPC)
     */
    public boolean sell(Player player, ItemStack item, int sellPrice) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Remove item from inventory
        player.getInventory().removeItem(item);
        
        // Give coins
        economyManager.deposit(player.getUniqueId(), sellPrice);
        
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("Vendiste ", NamedTextColor.GREEN))
                .append(Component.text(item.getType().name().replace("_", " "), NamedTextColor.YELLOW))
                .append(Component.text(" por ", NamedTextColor.GREEN))
                .append(Component.text(sellPrice + " coins", NamedTextColor.GOLD)));
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        return true;
    }
    
    // Getters
    public String getTradeId() {
        return tradeId;
    }
    
    public String getNpcName() {
        return npcName;
    }
    
    public List<TradeOffer> getOffers() {
        return offers;
    }
    
    /**
     * Represents a single trade offer
     */
    public static class TradeOffer {
        private final ItemStack item;
        private final ItemStack displayItem;
        private final int price;
        private int stock; // -1 for unlimited
        
        public TradeOffer(ItemStack item, int price) {
            this(item, price, -1);
        }
        
        public TradeOffer(ItemStack item, int price, int stock) {
            this.item = item;
            this.displayItem = item.clone();
            this.price = price;
            this.stock = stock;
        }
        
        public TradeOffer(ItemStack item, ItemStack displayItem, int price, int stock) {
            this.item = item;
            this.displayItem = displayItem;
            this.price = price;
            this.stock = stock;
        }
        
        public ItemStack getItem() {
            return item;
        }
        
        public ItemStack getDisplayItem() {
            return displayItem;
        }
        
        public int getPrice() {
            return price;
        }
        
        public int getStock() {
            return stock;
        }
        
        public void decreaseStock() {
            if (stock > 0) {
                stock--;
            }
        }
        
        public void setStock(int stock) {
            this.stock = stock;
        }
        
        public boolean isAvailable() {
            return stock != 0;
        }
    }
    
    /**
     * Builder for creating NPC trades
     */
    public static class Builder {
        private final NPCTrade trade;
        
        public Builder(String tradeId, String npcName, EconomyManager economyManager, MMORPGPlugin plugin) {
            this.trade = new NPCTrade(tradeId, npcName, economyManager, plugin);
        }
        
        public Builder addOffer(ItemStack item, int price) {
            trade.addOffer(new TradeOffer(item, price));
            return this;
        }
        
        public Builder addOffer(ItemStack item, int price, int stock) {
            trade.addOffer(new TradeOffer(item, price, stock));
            return this;
        }
        
        public Builder addOffer(TradeOffer offer) {
            trade.addOffer(offer);
            return this;
        }
        
        public NPCTrade build() {
            return trade;
        }
    }
}
