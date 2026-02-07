package com.nightslayer.mmorpg.integration;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages integrations like webhooks and external notifications
 */
public class IntegrationManager {

    private final DatabaseManager db;

    public IntegrationManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean addWebhook(String name, String url, String secret, String events) {
        String sql = "INSERT INTO integrations_webhooks (name, url, secret, events, is_active, created_at) VALUES (?, ?, ?, ?, 1, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, url);
            stmt.setString(3, secret);
            stmt.setString(4, events);
            stmt.setLong(5, System.currentTimeMillis());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error adding webhook", e);
        }
        return false;
    }

    public boolean removeWebhook(int id) {
        String sql = "DELETE FROM integrations_webhooks WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error removing webhook", e);
        }
        return false;
    }

    public List<Map<String, Object>> listWebhooks() {
        List<Map<String, Object>> hooks = new ArrayList<>();
        String sql = "SELECT id, name, url, events, is_active, created_at FROM integrations_webhooks";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> h = new HashMap<>();
                h.put("id", rs.getInt("id"));
                h.put("name", rs.getString("name"));
                h.put("url", rs.getString("url"));
                h.put("events", rs.getString("events"));
                h.put("is_active", rs.getInt("is_active") == 1);
                h.put("created_at", rs.getLong("created_at"));
                hooks.add(h);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing webhooks", e);
        }
        return hooks;
    }
}
