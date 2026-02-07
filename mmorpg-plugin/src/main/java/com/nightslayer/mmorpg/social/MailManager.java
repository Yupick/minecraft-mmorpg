package com.nightslayer.mmorpg.social;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages in-game mail
 */
public class MailManager {

    private final DatabaseManager db;

    public MailManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean sendMail(String senderUuid, String receiverUuid, String subject, String content) {
        String sql = "INSERT INTO mail_messages (sender_uuid, receiver_uuid, subject, content, sent_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, senderUuid);
            stmt.setString(2, receiverUuid);
            stmt.setString(3, subject);
            stmt.setString(4, content);
            stmt.setLong(5, System.currentTimeMillis());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error sending mail", e);
        }
        return false;
    }

    public List<Map<String, Object>> getInbox(String receiverUuid, int limit) {
        List<Map<String, Object>> inbox = new ArrayList<>();
        String sql = "SELECT id, sender_uuid, subject, content, sent_at, is_read FROM mail_messages " +
                     "WHERE receiver_uuid = ? ORDER BY sent_at DESC LIMIT ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, receiverUuid);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> mail = new HashMap<>();
                    mail.put("id", rs.getInt("id"));
                    mail.put("sender_uuid", rs.getString("sender_uuid"));
                    mail.put("subject", rs.getString("subject"));
                    mail.put("content", rs.getString("content"));
                    mail.put("sent_at", rs.getLong("sent_at"));
                    mail.put("is_read", rs.getInt("is_read") == 1);
                    inbox.add(mail);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting inbox", e);
        }
        return inbox;
    }

    public boolean markRead(int mailId) {
        String sql = "UPDATE mail_messages SET is_read = 1 WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, mailId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error marking mail read", e);
        }
        return false;
    }
}
