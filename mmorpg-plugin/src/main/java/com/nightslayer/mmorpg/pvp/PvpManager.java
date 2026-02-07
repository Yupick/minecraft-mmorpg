package com.nightslayer.mmorpg.pvp;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages PvP arenas, matches and rankings
 */
public class PvpManager {

    private final DatabaseManager db;

    public PvpManager(DatabaseManager db) {
        this.db = db;
    }

    public boolean createArena(String name, String world, double x, double y, double z, double radius) {
        String sql = "INSERT INTO pvp_arenas (name, world, x, y, z, radius, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, world);
            stmt.setDouble(3, x);
            stmt.setDouble(4, y);
            stmt.setDouble(5, z);
            stmt.setDouble(6, radius);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error creating arena", e);
        }
        return false;
    }

    public List<Map<String, Object>> listArenas() {
        List<Map<String, Object>> arenas = new ArrayList<>();
        String sql = "SELECT id, name, world, x, y, z, radius, is_active FROM pvp_arenas";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> a = new HashMap<>();
                a.put("id", rs.getInt("id"));
                a.put("name", rs.getString("name"));
                a.put("world", rs.getString("world"));
                a.put("x", rs.getDouble("x"));
                a.put("y", rs.getDouble("y"));
                a.put("z", rs.getDouble("z"));
                a.put("radius", rs.getDouble("radius"));
                a.put("is_active", rs.getInt("is_active") == 1);
                arenas.add(a);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing arenas", e);
        }
        return arenas;
    }

    public void recordMatch(Integer arenaId, String playerA, String playerB, String winnerUuid) {
        String insertMatch = "INSERT INTO pvp_matches (arena_id, player_a, player_b, winner_uuid, started_at, ended_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(insertMatch)) {
            long now = System.currentTimeMillis();
            stmt.setObject(1, arenaId);
            stmt.setString(2, playerA);
            stmt.setString(3, playerB);
            stmt.setString(4, winnerUuid);
            stmt.setLong(5, now);
            stmt.setLong(6, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error recording match", e);
        }

        updateRanking(playerA, playerB, winnerUuid);
    }

    private void updateRanking(String playerA, String playerB, String winnerUuid) {
        applyRatingChange(playerA, winnerUuid != null && winnerUuid.equals(playerA));
        applyRatingChange(playerB, winnerUuid != null && winnerUuid.equals(playerB));
    }

    private void applyRatingChange(String playerUuid, boolean win) {
        String upsert = "INSERT INTO pvp_rankings (player_uuid, rating, wins, losses, last_match) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET rating = rating + ?, wins = wins + ?, losses = losses + ?, last_match = ?";
        int delta = win ? 10 : -10;
        int winInc = win ? 1 : 0;
        int lossInc = win ? 0 : 1;
        long now = System.currentTimeMillis();
        try (PreparedStatement stmt = db.getConnection().prepareStatement(upsert)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, 1000 + delta);
            stmt.setInt(3, winInc);
            stmt.setInt(4, lossInc);
            stmt.setLong(5, now);
            stmt.setInt(6, delta);
            stmt.setInt(7, winInc);
            stmt.setInt(8, lossInc);
            stmt.setLong(9, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error updating ranking", e);
        }
    }

    public List<Map<String, Object>> getRankings(int limit) {
        List<Map<String, Object>> rankings = new ArrayList<>();
        String sql = "SELECT player_uuid, rating, wins, losses FROM pvp_rankings ORDER BY rating DESC LIMIT ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("player_uuid", rs.getString("player_uuid"));
                    r.put("rating", rs.getInt("rating"));
                    r.put("wins", rs.getInt("wins"));
                    r.put("losses", rs.getInt("losses"));
                    rankings.add(r);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting rankings", e);
        }
        return rankings;
    }
}
