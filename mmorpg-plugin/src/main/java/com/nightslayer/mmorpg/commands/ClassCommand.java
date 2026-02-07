package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public ClassCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("general.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(lang.getMessage("classes.choose_prompt"));
            return true;
        }
        
        String playerClass = args[0].toLowerCase();
        if (!playerClass.matches("warrior|mage|rogue|paladin")) {
            player.sendMessage(lang.getMessage("classes.choose_prompt"));
            return true;
        }
        
        // Update database
        String sql = "UPDATE players SET player_class = ? WHERE uuid = ?";
        plugin.getDatabaseManager().executeUpdate(sql, playerClass, player.getUniqueId().toString());
        
        player.sendMessage(lang.getMessage("classes.changed", "class", playerClass));
        return true;
    }
}
