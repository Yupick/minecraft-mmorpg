package com.nightslayer.mmorpg.social;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages friends system
 */
public class FriendsManager {

    private final DatabaseManager db;

    public FriendsManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean sendRequest(String fromUuid, String toUuid) {
        String sql = "INSERT OR IGNORE INTO friends (player_uuid, friend_uuid, status, created_at) VALUES (?, ?, 'pending', ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, fromUuid);
            stmt.setString(2, toUuid);
            stmt.setLong(3, System.currentTimeMillis());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error sending friend request", e);
        }
        return false;
    }

    public boolean acceptRequest(String fromUuid, String toUuid) {
        String sql = "UPDATE friends SET status = 'accepted' WHERE player_uuid = ? AND friend_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, fromUuid);
            stmt.setString(2, toUuid);
            if (stmt.executeUpdate() > 0) {
                // ensure reciprocal record
                String reverse = "INSERT OR IGNORE INTO friends (player_uuid, friend_uuid, status, created_at) VALUES (?, ?, 'accepted', ?)";
                try (PreparedStatement stmt2 = db.getConnection().prepareStatement(reverse)) {
                    stmt2.setString(1, toUuid);
                    stmt2.setString(2, fromUuid);
                    stmt2.setLong(3, System.currentTimeMillis());
                    stmt2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error accepting friend request", e);
        }
        return false;
    }

    public List<String> listFriends(String playerUuid) {
        List<String> friends = new ArrayList<>();
        String sql = "SELECT friend_uuid FROM friends WHERE player_uuid = ? AND status = 'accepted'";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("friend_uuid"));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing friends", e);
        }
        return friends;
    }
}
