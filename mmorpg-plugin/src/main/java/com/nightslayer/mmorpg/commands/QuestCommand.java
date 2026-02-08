package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.models.Quest;
import com.nightslayer.mmorpg.quests.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command for managing player quests
 * Usage: /quest <list|start|progress|abandon|info>
 */
public class QuestCommand implements CommandExecutor, TabCompleter {
    private final QuestManager questManager;
    
    public QuestCommand(MMORPGPlugin plugin) {
        this.questManager = plugin.getQuestManager();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.", NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list" -> handleList(player);
            case "start" -> handleStart(player, args);
            case "progress" -> handleProgress(player);
            case "abandon" -> handleAbandon(player, args);
            case "info" -> handleInfo(player, args);
            default -> sendUsage(player);
        }
        
        return true;
    }
    
    /**
     * Shows available quests
     */
    private void handleList(Player player) {
        List<Quest> availableQuests = questManager.getAvailableQuests(player);
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("      QUESTS DISPONIBLES      ", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        
        if (availableQuests.isEmpty()) {
            player.sendMessage(Component.text("  No hay quests disponibles en este momento.", NamedTextColor.GRAY));
        } else {
            for (Quest quest : availableQuests) {
                Component questLine = Component.text("  • ", NamedTextColor.YELLOW)
                        .append(Component.text(quest.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" [Nivel " + quest.getMinLevel() + "]", NamedTextColor.GRAY))
                        .hoverEvent(HoverEvent.showText(Component.text("Click para más info", NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.runCommand("/quest info " + quest.getId()));
                
                player.sendMessage(questLine);
            }
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("  Tip: Haz click en una quest o usa /quest info <id>", NamedTextColor.GRAY, TextDecoration.ITALIC));
        }
        
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }
    
    /**
     * Starts a quest
     */
    private void handleStart(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest start <id>", NamedTextColor.RED));
            return;
        }
        
        int questId;
        try {
            questId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("ID de quest inválido.", NamedTextColor.RED));
            return;
        }
        
        if (questManager.startQuest(player, questId)) {
            Quest quest = questManager.getQuestById(questId);
            if (quest != null) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Quest iniciada: ", NamedTextColor.GREEN))
                        .append(Component.text(quest.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
                player.sendMessage(Component.text("  " + quest.getDescription(), NamedTextColor.GRAY));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        } else {
            player.sendMessage(Component.text("No puedes iniciar esta quest.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Shows quest progress
     */
    private void handleProgress(Player player) {
        List<Quest> activeQuests = questManager.getActiveQuests(player.getUniqueId());
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.AQUA));
        player.sendMessage(Component.text("      TU PROGRESO DE QUESTS      ", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.AQUA));
        player.sendMessage(Component.text(""));
        
        if (activeQuests.isEmpty()) {
            player.sendMessage(Component.text("  No tienes quests activas.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Usa /quest list para ver quests disponibles.", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
            for (Quest quest : activeQuests) {
                int progress = questManager.getQuestProgress(player.getUniqueId(), quest.getId());
                String progressBar = createProgressBar(progress, 20);
                
                player.sendMessage(Component.text("  " + quest.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
                player.sendMessage(Component.text("    " + progressBar + " " + progress + "%", NamedTextColor.YELLOW));
                player.sendMessage(Component.text(""));
            }
        }
        
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }
    
    /**
     * Abandons a quest
     */
    private void handleAbandon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest abandon <id>", NamedTextColor.RED));
            return;
        }
        
        int questId;
        try {
            questId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("ID de quest inválido.", NamedTextColor.RED));
            return;
        }
        
        if (questManager.abandonQuest(player.getUniqueId(), questId)) {
            player.sendMessage(Component.text("✗ Quest abandonada.", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        } else {
            player.sendMessage(Component.text("No tienes esta quest activa.", NamedTextColor.RED));
        }
    }
    
    /**
     * Shows detailed quest information
     */
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest info <id>", NamedTextColor.RED));
            return;
        }
        
        int questId;
        try {
            questId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("ID de quest inválido.", NamedTextColor.RED));
            return;
        }
        
        Quest quest = questManager.getQuestById(questId);
        if (quest == null) {
            player.sendMessage(Component.text("Quest no encontrada.", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(quest.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Descripción:", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  " + quest.getDescription(), NamedTextColor.WHITE));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Nivel requerido: ", NamedTextColor.GRAY)
            .append(Component.text(quest.getMinLevel(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text(""));
        
        // Show start button if not started
        boolean hasQuest = questManager.getActiveQuests(player.getUniqueId()).stream()
            .anyMatch(q -> q.getId() == questId);
        
        if (!hasQuest) {
            Component startButton = Component.text("[INICIAR QUEST]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Click para iniciar", NamedTextColor.GREEN)))
                    .clickEvent(ClickEvent.runCommand("/quest start " + questId));
            player.sendMessage(Component.text("  ").append(startButton));
        } else {
            player.sendMessage(Component.text("  ✓ Quest activa", NamedTextColor.GREEN));
        }
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }
    
    /**
     * Creates a visual progress bar
     */
    private String createProgressBar(int percentage, int bars) {
        int filled = (int) ((percentage / 100.0) * bars);
        StringBuilder bar = new StringBuilder("§a");
        
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("§7█");
            }
        }
        
        return bar.toString();
    }
    
    /**
     * Sends command usage
     */
    private void sendUsage(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══ Quest Command ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/quest list", NamedTextColor.YELLOW)
                .append(Component.text(" - Ver quests disponibles", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest start <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Iniciar una quest", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest progress", NamedTextColor.YELLOW)
                .append(Component.text(" - Ver tu progreso", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest info <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Info de quest", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest abandon <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Abandonar quest", NamedTextColor.GRAY)));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "start", "progress", "abandon", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || 
                                  args[0].equalsIgnoreCase("info") || 
                                  args[0].equalsIgnoreCase("abandon"))) {
            // Could return quest IDs here
            return Collections.emptyList();
        }
        
        return Collections.emptyList();
    }
}
