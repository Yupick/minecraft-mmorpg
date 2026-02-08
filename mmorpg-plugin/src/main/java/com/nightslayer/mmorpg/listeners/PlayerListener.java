package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final DatabaseManager db;
    
    public PlayerListener(MMORPGPlugin plugin) {
        this.db = plugin.getDatabaseManager();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        
        // Create player record if not exists
        String sql = "INSERT OR IGNORE INTO players (uuid, username, player_class, level, experience) VALUES (?, ?, 'none', 1, 0)";
        db.executeUpdate(sql, uuid, event.getPlayer().getName());
        
        // Create economy record
        sql = "INSERT OR IGNORE INTO player_economy (player_uuid, coins) VALUES (?, 100)";
        db.executeUpdate(sql, uuid);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data
        // TODO: Implement auto-save
    }
}
