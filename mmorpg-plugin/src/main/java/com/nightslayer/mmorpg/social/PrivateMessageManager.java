package com.nightslayer.mmorpg.social;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages private messages between players
 */
public class PrivateMessageManager {

    private final DatabaseManager db;

    public PrivateMessageManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean sendMessage(String senderUuid, String receiverUuid, String content) {
        String sql = "INSERT INTO private_messages (sender_uuid, receiver_uuid, content, sent_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, senderUuid);
            stmt.setString(2, receiverUuid);
            stmt.setString(3, content);
            stmt.setLong(4, System.currentTimeMillis());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error sending private message", e);
        }
        return false;
    }

    public List<Map<String, Object>> getConversation(String aUuid, String bUuid, int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT sender_uuid, receiver_uuid, content, sent_at, is_read FROM private_messages " +
                     "WHERE (sender_uuid = ? AND receiver_uuid = ?) OR (sender_uuid = ? AND receiver_uuid = ?) " +
                     "ORDER BY sent_at DESC LIMIT ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, aUuid);
            stmt.setString(2, bUuid);
            stmt.setString(3, bUuid);
            stmt.setString(4, aUuid);
            stmt.setInt(5, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("sender_uuid", rs.getString("sender_uuid"));
                    msg.put("receiver_uuid", rs.getString("receiver_uuid"));
                    msg.put("content", rs.getString("content"));
                    msg.put("sent_at", rs.getLong("sent_at"));
                    msg.put("is_read", rs.getInt("is_read") == 1);
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting conversation", e);
        }
        return messages;
    }
}
