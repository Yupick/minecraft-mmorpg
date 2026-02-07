package com.nightslayer.mmorpg.economy;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;

import java.sql.ResultSet;
import java.util.UUID;

public class EconomyManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public EconomyManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }
    
    public int getBalance(UUID playerId) {
        String sql = "SELECT coins FROM player_economy WHERE player_uuid = ?";
        try (ResultSet rs = db.executeQuery(sql, playerId.toString())) {
            if (rs.next()) {
                return rs.getInt("coins");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean deposit(UUID playerId, int amount) {
        String sql = "UPDATE player_economy SET coins = coins + ? WHERE player_uuid = ?";
        return db.executeUpdate(sql, amount, playerId.toString()) > 0;
    }
    
    public boolean withdraw(UUID playerId, int amount) {
        if (getBalance(playerId) < amount) {
            return false;
        }
        String sql = "UPDATE player_economy SET coins = coins - ? WHERE player_uuid = ?";
        return db.executeUpdate(sql, amount, playerId.toString()) > 0;
    }
    
    public boolean transfer(UUID from, UUID to, int amount) {
        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                // Log transaction
                String sql = "INSERT INTO transactions (from_player, to_player, amount, type) VALUES (?, ?, ?, 'transfer')";
                db.executeUpdate(sql, from.toString(), to.toString(), amount);
                return true;
            } else {
                // Rollback
                deposit(from, amount);
            }
        }
        return false;
    }
}
