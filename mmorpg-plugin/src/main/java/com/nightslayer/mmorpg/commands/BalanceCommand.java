package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public BalanceCommand(MMORPGPlugin plugin) {
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
        int balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        player.sendMessage(lang.getMessage("economy.balance", "coins", balance));
        
        return true;
    }
}
