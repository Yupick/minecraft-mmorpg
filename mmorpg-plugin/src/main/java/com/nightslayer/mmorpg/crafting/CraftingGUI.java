package com.nightslayer.mmorpg.crafting;

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
 * GUI interface for the custom crafting system
 */
public class CraftingGUI implements Listener {
    
    private final MMORPGPlugin plugin;
    private final CraftingManager craftingManager;
    private final EconomyManager economyManager;
    private final Map<UUID, Inventory> activeGUIs;
    
    // GUI slots
    private static final int[] RECIPE_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 24;
    private static final int INFO_SLOT = 4;
    private static final int CRAFT_BUTTON_SLOT = 49;
    
    public CraftingGUI(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.craftingManager = plugin.getCraftingManager();
        this.economyManager = plugin.getEconomyManager();
        this.activeGUIs = new HashMap<>();
    }
    
    /**
     * Opens the crafting GUI for a player
     */
    public void openCraftingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, 
                Component.text("Mesa de Crafteo RPG", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        
        // Fill borders with glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            if (!isRecipeSlot(i) && i != RESULT_SLOT && i != CRAFT_BUTTON_SLOT) {
                gui.setItem(i, border);
            }
        }
        
        // Set info item
        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(meta -> {
            meta.displayName(Component.text("Instrucciones", NamedTextColor.YELLOW, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("1. Coloca los materiales en la cuadrícula", NamedTextColor.GRAY));
            lore.add(Component.text("2. El resultado aparecerá a la derecha", NamedTextColor.GRAY));
            lore.add(Component.text("3. Haz click en CRAFTEAR para crear", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Algunas recetas requieren coins o XP", NamedTextColor.GOLD));
            meta.lore(lore);
        });
        gui.setItem(INFO_SLOT, info);
        
        // Set craft button
        ItemStack craftButton = new ItemStack(Material.CRAFTING_TABLE);
        craftButton.editMeta(meta -> {
            meta.displayName(Component.text("CRAFTEAR", NamedTextColor.GREEN, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Click para craftear el item", NamedTextColor.GRAY));
            meta.lore(lore);
        });
        gui.setItem(CRAFT_BUTTON_SLOT, craftButton);
        
        activeGUIs.put(player.getUniqueId(), gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }
    
    /**
     * Checks if a slot is a recipe slot
     */
    private boolean isRecipeSlot(int slot) {
        for (int recipeSlot : RECIPE_SLOTS) {
            if (slot == recipeSlot) return true;
        }
        return false;
    }
    
    /**
     * Handles GUI clicks
     */
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
        
        // Allow clicks in recipe slots
        if (isRecipeSlot(slot)) {
            // Update recipe preview after a tick
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateRecipePreview(player, gui), 1L);
            return;
        }
        
        // Handle craft button click
        if (slot == CRAFT_BUTTON_SLOT) {
            event.setCancelled(true);
            handleCraft(player, gui);
            return;
        }
        
        // Cancel all other clicks
        event.setCancelled(true);
    }
    
    /**
     * Updates the recipe preview based on current materials
     */
    private void updateRecipePreview(Player player, Inventory gui) {
        // Get materials from recipe slots
        Map<Material, Integer> materials = new HashMap<>();
        for (int slot : RECIPE_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        // Find matching recipe
        CraftingManager.CraftingRecipe recipe = findMatchingRecipe(player, materials);
        
        if (recipe != null) {
            // Show result
            Material resultMaterial = Material.getMaterial(recipe.getResultItem().toUpperCase());
            if (resultMaterial == null) {
                gui.setItem(RESULT_SLOT, null);
                return;
            }
            ItemStack result = new ItemStack(resultMaterial, recipe.getResultAmount());
            result.editMeta(meta -> {
                List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : new ArrayList<>());
                lore.add(Component.text(""));
                if (recipe.getCoinCost() > 0) {
                    lore.add(Component.text("Costo: " + recipe.getCoinCost() + " coins", NamedTextColor.GOLD));
                }
                if (recipe.getExpCost() > 0) {
                    lore.add(Component.text("Costo: " + recipe.getExpCost() + " XP", NamedTextColor.AQUA));
                }
                meta.lore(lore);
            });
            gui.setItem(RESULT_SLOT, result);
        } else {
            // No matching recipe
            gui.setItem(RESULT_SLOT, null);
        }
    }
    
    /**
     * Handles crafting attempt
     */
    private void handleCraft(Player player, Inventory gui) {
        // Get materials from recipe slots
        Map<Material, Integer> materials = new HashMap<>();
        for (int slot : RECIPE_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        if (materials.isEmpty()) {
            player.sendMessage(Component.text("Coloca materiales en la cuadrícula primero.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Find matching recipe
        CraftingManager.CraftingRecipe recipe = findMatchingRecipe(player, materials);
        
        if (recipe == null) {
            player.sendMessage(Component.text("Receta desconocida. Verifica los materiales.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Check coin cost
        if (recipe.getCoinCost() > 0) {
            int balance = economyManager.getBalance(player.getUniqueId());
            if (balance < recipe.getCoinCost()) {
                player.sendMessage(Component.text("No tienes suficientes coins. Necesitas: " + recipe.getCoinCost(), NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }
        
        // Check XP cost
        if (recipe.getExpCost() > 0) {
            if (player.getTotalExperience() < recipe.getExpCost()) {
                player.sendMessage(Component.text("No tienes suficiente XP. Necesitas: " + recipe.getExpCost(), NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }
        
        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Tu inventario está lleno.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Deduct costs
        if (recipe.getCoinCost() > 0) {
            economyManager.withdraw(player.getUniqueId(), recipe.getCoinCost());
        }
        if (recipe.getExpCost() > 0) {
            player.giveExp(-recipe.getExpCost());
        }
        
        // Remove materials
        for (int slot : RECIPE_SLOTS) {
            gui.setItem(slot, null);
        }
        
        // Give result
        Material resultMaterial = Material.getMaterial(recipe.getResultItem().toUpperCase());
        if (resultMaterial != null) {
            player.getInventory().addItem(new ItemStack(resultMaterial, recipe.getResultAmount()));
        }
        
        // Clear result preview
        gui.setItem(RESULT_SLOT, null);
        
        // Send success message
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("Item crafteado: ", NamedTextColor.GREEN))
                .append(Component.text(recipe.getResultItem().replace("_", " "), NamedTextColor.YELLOW)));
        
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
    }
    
    /**
     * Handles GUI close
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Inventory gui = activeGUIs.remove(player.getUniqueId());
        if (gui == null) return;
        
        // Return materials to player
        for (int slot : RECIPE_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
    }

    private CraftingManager.CraftingRecipe findMatchingRecipe(Player player, Map<Material, Integer> materials) {
        for (CraftingManager.CraftingRecipe recipe : craftingManager.getAvailableRecipes(player)) {
            if (recipe.getParsedMaterials().equals(materials)) {
                return recipe;
            }
        }
        return null;
    }
}
