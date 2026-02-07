package com.nightslayer.mmorpg.optimization;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages optimization data, metrics and backups registry
 */
public class OptimizationManager {

    private final DatabaseManager db;

    public OptimizationManager(DatabaseManager db) {
        this.db = db;
    }

    public void recordMetric(String key, double value) {
        String sql = "INSERT INTO metrics (key, value, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setDouble(2, value);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error recording metric", e);
        }
    }

    public List<Map<String, Object>> listMetrics(int limit) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        String sql = "SELECT key, value, timestamp FROM metrics ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("key", rs.getString("key"));
                    m.put("value", rs.getDouble("value"));
                    m.put("timestamp", rs.getLong("timestamp"));
                    metrics.add(m);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing metrics", e);
        }
        return metrics;
    }

    public void registerBackup(String path, long size, String status) {
        String sql = "INSERT INTO backups (path, size, created_at, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.setLong(2, size);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, status);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error registering backup", e);
        }
    }

    public List<Map<String, Object>> listBackups(int limit) {
        List<Map<String, Object>> backups = new ArrayList<>();
        String sql = "SELECT id, path, size, created_at, status FROM backups ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", rs.getInt("id"));
                    b.put("path", rs.getString("path"));
                    b.put("size", rs.getLong("size"));
                    b.put("created_at", rs.getLong("created_at"));
                    b.put("status", rs.getString("status"));
                    backups.add(b);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing backups", e);
        }
        return backups;
    }
}
