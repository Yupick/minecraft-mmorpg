package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

public class StatsCommand implements CommandExecutor {
    private final MMORPGPlugin plugin;
    private final LanguageManager lang;
    
    public StatsCommand(MMORPGPlugin plugin) {
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
        String sql = "SELECT * FROM players WHERE uuid = ?";
        
        try (ResultSet rs = plugin.getDatabaseManager().executeQuery(sql, player.getUniqueId().toString())) {
            if (rs.next()) {
                player.sendMessage(lang.getMessage("stats.title", "player", player.getName()));
                player.sendMessage(lang.getMessage("stats.level", "level", rs.getInt("level")));
                player.sendMessage(lang.getMessage("stats.experience", "exp", rs.getInt("experience"), "max_exp", 1000));
            }
        } catch (Exception e) {
            player.sendMessage(lang.getMessage("general.error"));
            e.printStackTrace();
        }
        
        return true;
    }
}
