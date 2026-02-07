package com.nightslayer.mmorpg.enchanting;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI interface for the enchantment system
 */
public class EnchantingGUI implements Listener {
    
    private final MMORPGPlugin plugin;
    private final EnchantmentManager enchantmentManager;
    private final EconomyManager economyManager;
    private final Map<UUID, Inventory> activeGUIs;
    private final Map<UUID, ItemStack> itemsToEnchant;
    
    private static final int ITEM_SLOT = 13;
    private static final int INFO_SLOT = 4;
    private static final int[] ENCHANT_SLOTS = {29, 30, 31, 32, 33};
    
    public EnchantingGUI(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.enchantmentManager = plugin.getEnchantmentManager();
        this.economyManager = plugin.getEconomyManager();
        this.activeGUIs = new HashMap<>();
        this.itemsToEnchant = new HashMap<>();
    }
    
    /**
     * Opens the enchanting GUI
     */
    public void openEnchantingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text("Mesa de Encantamientos RPG", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        
        // Fill borders
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            if (i != ITEM_SLOT && i != INFO_SLOT && !isEnchantSlot(i)) {
                gui.setItem(i, border);
            }
        }
        
        // Set info item
        ItemStack info = new ItemStack(Material.ENCHANTED_BOOK);
        info.editMeta(meta -> {
            meta.displayName(Component.text("Cómo Encantar", NamedTextColor.AQUA, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("1. Coloca un item en el slot central", NamedTextColor.GRAY));
            lore.add(Component.text("2. Selecciona un encantamiento", NamedTextColor.GRAY));
            lore.add(Component.text("3. Paga el costo en coins/XP", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Los encantamientos requieren nivel", NamedTextColor.YELLOW));
            meta.lore(lore);
        });
        gui.setItem(INFO_SLOT, info);
        
        // Set item slot placeholder
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        placeholder.editMeta(meta -> {
            meta.displayName(Component.text("Coloca un item aquí", NamedTextColor.RED));
        });
        gui.setItem(ITEM_SLOT, placeholder);
        
        activeGUIs.put(player.getUniqueId(), gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }
    
    private boolean isEnchantSlot(int slot) {
        for (int enchantSlot : ENCHANT_SLOTS) {
            if (slot == enchantSlot) return true;
        }
        return false;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeGUIs.containsKey(player.getUniqueId())) return;
        
        Inventory gui = activeGUIs.get(player.getUniqueId());
        if (!event.getInventory().equals(gui)) return;
        
        int slot = event.getRawSlot();
        
        // Allow clicks in player inventory
        if (slot >= 54) {
            return;
        }
        
        // Handle item slot
        if (slot == ITEM_SLOT) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            
            if (cursor != null && cursor.getType() != Material.AIR) {
                // Placing item
                itemsToEnchant.put(player.getUniqueId(), cursor.clone());
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateEnchantmentOptions(player, gui), 1L);
            } else if (current != null && current.getType() != Material.AIR && current.getType() != Material.BARRIER) {
                // Taking item back
                itemsToEnchant.remove(player.getUniqueId());
                clearEnchantmentOptions(gui);
            }
            return;
        }
        
        // Handle enchantment selection
        if (isEnchantSlot(slot)) {
            event.setCancelled(true);
            handleEnchantmentClick(player, gui, slot);
            return;
        }
        
        event.setCancelled(true);
    }
    
    private void updateEnchantmentOptions(Player player, Inventory gui) {
        ItemStack item = itemsToEnchant.get(player.getUniqueId());
        if (item == null) return;
        
        List<EnchantmentManager.RPGEnchantment> available = enchantmentManager.getAvailableEnchantments(player);
        
        // Clear previous options
        clearEnchantmentOptions(gui);
        
        // Show available enchantments
        int slotIndex = 0;
        for (EnchantmentManager.RPGEnchantment enchant : available) {
            if (!enchant.isApplicable(item)) {
                continue;
            }
            if (slotIndex >= ENCHANT_SLOTS.length) break;
            
            ItemStack enchantOption = new ItemStack(Material.ENCHANTED_BOOK);
            enchantOption.editMeta(meta -> {
                meta.displayName(Component.text(enchant.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text("Tipo: " + enchant.getType(), NamedTextColor.GRAY));
                lore.add(Component.text(""));
                lore.add(Component.text("Nivel requerido: " + enchant.getLevelRequired(), NamedTextColor.AQUA));
                lore.add(Component.text("Costo: " + enchant.getCoinCostPerLevel() + " coins", NamedTextColor.GOLD));
                lore.add(Component.text("XP por nivel: " + enchant.getExpCostPerLevel(), NamedTextColor.AQUA));
                lore.add(Component.text(""));
                lore.add(Component.text("Click para aplicar", NamedTextColor.GREEN));
                meta.lore(lore);
            });
            
            gui.setItem(ENCHANT_SLOTS[slotIndex], enchantOption);
            slotIndex++;
        }
    }
    
    private void clearEnchantmentOptions(Inventory gui) {
        for (int slot : ENCHANT_SLOTS) {
            gui.setItem(slot, null);
        }
    }
    
    private void handleEnchantmentClick(Player player, Inventory gui, int slot) {
        ItemStack item = itemsToEnchant.get(player.getUniqueId());
        if (item == null) {
            player.sendMessage(Component.text("Primero coloca un item.", NamedTextColor.RED));
            return;
        }
        
        ItemStack enchantOption = gui.getItem(slot);
        if (enchantOption == null) return;
        
        // Get enchantment from display name
        String enchantName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(enchantOption.getItemMeta().displayName());
        
        // Find enchantment
        EnchantmentManager.RPGEnchantment enchant = enchantmentManager.getAvailableEnchantments(player)
                .stream()
                .filter(e -> e.getName().equals(enchantName))
                .findFirst()
                .orElse(null);
        
        if (enchant == null) return;
        
        // Check balance
        int balance = economyManager.getBalance(player.getUniqueId());
        if (balance < enchant.getCoinCostPerLevel()) {
            player.sendMessage(Component.text("No tienes suficientes coins. Necesitas: " + enchant.getCoinCostPerLevel(), NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Apply enchantment
        if (enchantmentManager.applyEnchantment(player, item, enchant.getEnchantmentId(), 1)) {
            // Deduct cost
            economyManager.withdraw(player.getUniqueId(), enchant.getCoinCostPerLevel());
            
            // Update item in slot
            gui.setItem(ITEM_SLOT, item);
            
            player.sendMessage(Component.text("✓ Encantamiento aplicado: ", NamedTextColor.GREEN)
                    .append(Component.text(enchant.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
            
            // Refresh options
            updateEnchantmentOptions(player, gui);
        } else {
            player.sendMessage(Component.text("No se pudo aplicar el encantamiento.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Inventory gui = activeGUIs.remove(player.getUniqueId());
        if (gui == null) return;
        
        // Return item
        ItemStack item = itemsToEnchant.remove(player.getUniqueId());
        if (item != null) {
            player.getInventory().addItem(item);
        }
    }
}
