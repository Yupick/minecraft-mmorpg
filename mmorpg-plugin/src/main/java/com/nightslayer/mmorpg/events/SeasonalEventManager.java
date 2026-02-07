package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages seasonal and server events
 */
public class SeasonalEventManager {

    private final DatabaseManager db;

    public SeasonalEventManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean createEvent(String name, String type, String configJson, long startTime, long endTime) {
        String sql = "INSERT INTO events (name, type, status, start_time, end_time, config_json) VALUES (?, ?, 'scheduled', ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, type);
            stmt.setLong(3, startTime);
            stmt.setLong(4, endTime);
            stmt.setString(5, configJson);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error creating event", e);
        }
        return false;
    }

    public boolean updateEventStatus(int eventId, String status) {
        String sql = "UPDATE events SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error updating event status", e);
        }
        return false;
    }

    public List<Map<String, Object>> listEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        String sql = "SELECT id, name, type, status, start_time, end_time FROM events ORDER BY start_time DESC";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> e = new HashMap<>();
                e.put("id", rs.getInt("id"));
                e.put("name", rs.getString("name"));
                e.put("type", rs.getString("type"));
                e.put("status", rs.getString("status"));
                e.put("start_time", rs.getLong("start_time"));
                e.put("end_time", rs.getLong("end_time"));
                events.add(e);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing events", e);
        }
        return events;
    }
}
