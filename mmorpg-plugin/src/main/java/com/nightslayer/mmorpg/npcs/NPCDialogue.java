package com.nightslayer.mmorpg.npcs;

import org.bukkit.entity.Player;
import org.bukkit.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

/**
 * Manages NPC dialogue trees with branching conversations
 */
public class NPCDialogue {
    
    private final String dialogueId;
    private final String npcName;
    private final List<DialoguePage> pages;
    private int currentPage;
    
    public NPCDialogue(String dialogueId, String npcName) {
        this.dialogueId = dialogueId;
        this.npcName = npcName;
        this.pages = new ArrayList<>();
        this.currentPage = 0;
    }
    
    /**
     * Adds a page to the dialogue
     */
    public void addPage(DialoguePage page) {
        this.pages.add(page);
    }
    
    /**
     * Displays the current dialogue page to the player
     */
    public void show(Player player) {
        if (pages.isEmpty()) {
            player.sendMessage(Component.text("Este NPC no tiene nada que decir.", NamedTextColor.RED));
            return;
        }
        
        if (currentPage >= pages.size()) {
            player.sendMessage(Component.text("Fin del diálogo.", NamedTextColor.GRAY));
            return;
        }
        
        DialoguePage page = pages.get(currentPage);
        
        // Send dialogue header
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text(npcName, NamedTextColor.YELLOW, TextDecoration.BOLD)
                .append(Component.text(" dice:", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        
        // Send dialogue text
        for (String line : page.getText()) {
            player.sendMessage(Component.text("  " + line, NamedTextColor.WHITE));
        }
        player.sendMessage(Component.text(""));
        
        // Show options if available
        if (!page.getOptions().isEmpty()) {
            player.sendMessage(Component.text("Opciones:", NamedTextColor.AQUA, TextDecoration.BOLD));
            int optionIndex = 1;
            for (DialogueOption option : page.getOptions()) {
                player.sendMessage(Component.text("  " + optionIndex + ". ", NamedTextColor.GOLD)
                        .append(Component.text(option.getText(), NamedTextColor.YELLOW)));
                optionIndex++;
            }
        } else {
            player.sendMessage(Component.text("  [Presiona SHIFT + Click derecho para continuar]", NamedTextColor.GRAY));
        }
        
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.DARK_GRAY));
        
        // Play sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
    }
    
    /**
     * Selects an option and advances dialogue
     */
    public void selectOption(Player player, int optionIndex) {
        if (currentPage >= pages.size()) {
            return;
        }
        
        DialoguePage page = pages.get(currentPage);
        List<DialogueOption> options = page.getOptions();
        
        if (optionIndex < 0 || optionIndex >= options.size()) {
            player.sendMessage(Component.text("Opción inválida.", NamedTextColor.RED));
            return;
        }
        
        DialogueOption option = options.get(optionIndex);
        
        // Execute option action
        if (option.getAction() != null) {
            option.getAction().accept(player);
        }
        
        // Move to next page or end dialogue
        if (option.getNextPageId() != -1) {
            currentPage = option.getNextPageId();
            show(player);
        } else {
            // End dialogue
            player.sendMessage(Component.text("Fin del diálogo.", NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        }
    }
    
    /**
     * Advances to the next page in linear dialogue
     */
    public void nextPage(Player player) {
        currentPage++;
        show(player);
    }
    
    /**
     * Resets dialogue to the beginning
     */
    public void reset() {
        this.currentPage = 0;
    }
    
    /**
     * Gets the current page index
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Checks if dialogue has ended
     */
    public boolean isFinished() {
        return currentPage >= pages.size();
    }
    
    // Getters
    public String getDialogueId() {
        return dialogueId;
    }
    
    public String getNpcName() {
        return npcName;
    }
    
    /**
     * Represents a single page in a dialogue tree
     */
    public static class DialoguePage {
        private final List<String> text;
        private final List<DialogueOption> options;
        
        public DialoguePage(String... text) {
            this.text = Arrays.asList(text);
            this.options = new ArrayList<>();
        }
        
        public DialoguePage(List<String> text) {
            this.text = new ArrayList<>(text);
            this.options = new ArrayList<>();
        }
        
        public void addOption(DialogueOption option) {
            this.options.add(option);
        }
        
        public List<String> getText() {
            return text;
        }
        
        public List<DialogueOption> getOptions() {
            return options;
        }
    }
    
    /**
     * Represents a dialogue option/choice
     */
    public static class DialogueOption {
        private final String text;
        private final int nextPageId;
        private final java.util.function.Consumer<Player> action;
        
        public DialogueOption(String text, int nextPageId) {
            this(text, nextPageId, null);
        }
        
        public DialogueOption(String text, int nextPageId, java.util.function.Consumer<Player> action) {
            this.text = text;
            this.nextPageId = nextPageId;
            this.action = action;
        }
        
        public String getText() {
            return text;
        }
        
        public int getNextPageId() {
            return nextPageId;
        }
        
        public java.util.function.Consumer<Player> getAction() {
            return action;
        }
    }
    
    /**
     * Builder for creating complex dialogues
     */
    public static class Builder {
        private final NPCDialogue dialogue;
        
        public Builder(String dialogueId, String npcName) {
            this.dialogue = new NPCDialogue(dialogueId, npcName);
        }
        
        public Builder addPage(String... text) {
            dialogue.addPage(new DialoguePage(text));
            return this;
        }
        
        public Builder addPageWithOptions(DialoguePage page) {
            dialogue.addPage(page);
            return this;
        }
        
        public NPCDialogue build() {
            return dialogue;
        }
    }
}
