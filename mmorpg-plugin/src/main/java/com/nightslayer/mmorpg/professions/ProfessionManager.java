package com.nightslayer.mmorpg.professions;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages professions progression
 */
public class ProfessionManager {

    private final DatabaseManager db;

    public ProfessionManager(DatabaseManager db) {
        this.db = db;
        seedDefaultProfessions();
    }

    private void seedDefaultProfessions() {
        String[] defaults = new String[]{"Minería", "Herrería", "Alquimia", "Encantamiento", "Cocina"};
        for (String name : defaults) {
            String sql = "INSERT OR IGNORE INTO professions (name, description, max_level) VALUES (?, ?, 100)";
            try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, "Profesión: " + name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error seeding professions", e);
            }
        }
    }

    public List<Map<String, Object>> listProfessions() {
        List<Map<String, Object>> professions = new ArrayList<>();
        String sql = "SELECT id, name, description, max_level FROM professions";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", rs.getInt("id"));
                p.put("name", rs.getString("name"));
                p.put("description", rs.getString("description"));
                p.put("max_level", rs.getInt("max_level"));
                professions.add(p);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing professions", e);
        }
        return professions;
    }

    public List<Map<String, Object>> getPlayerProfessions(String playerUuid) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.id, p.name, pp.level, pp.experience FROM player_professions pp JOIN professions p ON pp.profession_id = p.id WHERE pp.player_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("name", rs.getString("name"));
                    row.put("level", rs.getInt("level"));
                    row.put("experience", rs.getInt("experience"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player professions", e);
        }
        return list;
    }

    public boolean setProfessionProgress(String playerUuid, int professionId, int level, int experience) {
        String sql = "INSERT INTO player_professions (player_uuid, profession_id, level, experience, last_updated) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(player_uuid, profession_id) DO UPDATE SET level = ?, experience = ?, last_updated = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            stmt.setString(1, playerUuid);
            stmt.setInt(2, professionId);
            stmt.setInt(3, level);
            stmt.setInt(4, experience);
            stmt.setLong(5, now);
            stmt.setInt(6, level);
            stmt.setInt(7, experience);
            stmt.setLong(8, now);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error setting profession progress", e);
        }
        return false;
    }
}
