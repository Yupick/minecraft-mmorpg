package com.nightslayer.mmorpg.squads;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;

import java.util.UUID;

public class SquadManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    
    public SquadManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }
    
    public boolean createSquad(UUID leaderId, String squadName) {
        String sql = "INSERT INTO squads (squad_name, leader_uuid, max_members, bank_balance) VALUES (?, ?, 10, 0)";
        if (db.executeUpdate(sql, squadName, leaderId.toString()) > 0) {
            // Add leader as member
            sql = "INSERT INTO squad_members (squad_id, player_uuid, rank) VALUES (last_insert_rowid(), ?, 'leader')";
            db.executeUpdate(sql, leaderId.toString());
            return true;
        }
        return false;
    }
    
    public boolean addMember(int squadId, UUID playerId) {
        String sql = "INSERT INTO squad_members (squad_id, player_uuid, rank) VALUES (?, ?, 'member')";
        return db.executeUpdate(sql, squadId, playerId.toString()) > 0;
    }
}
