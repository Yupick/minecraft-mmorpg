package com.nightslayer.mmorpg.social;

import com.nightslayer.mmorpg.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages guilds/clans
 */
public class GuildManager {

    private final DatabaseManager db;

    public GuildManager(DatabaseManager db) {
        this.db = db;
    }

    public int createGuild(String name, String tag, String leaderUuid, String description) {
        String sql = "INSERT INTO guilds (name, tag, leader_uuid, description, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, tag);
            stmt.setString(3, leaderUuid);
            stmt.setString(4, description);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error creating guild", e);
            return -1;
        }

        return getGuildIdByName(name);
    }

    public int getGuildIdByName(String name) {
        String sql = "SELECT id FROM guilds WHERE name = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting guild id", e);
        }
        return -1;
    }

    public List<Map<String, Object>> listGuilds() {
        List<Map<String, Object>> guilds = new ArrayList<>();
        String sql = "SELECT id, name, tag, leader_uuid, description, max_members, bank_balance FROM guilds";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> g = new HashMap<>();
                g.put("id", rs.getInt("id"));
                g.put("name", rs.getString("name"));
                g.put("tag", rs.getString("tag"));
                g.put("leader_uuid", rs.getString("leader_uuid"));
                g.put("description", rs.getString("description"));
                g.put("max_members", rs.getInt("max_members"));
                g.put("bank_balance", rs.getDouble("bank_balance"));
                guilds.add(g);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing guilds", e);
        }
        return guilds;
    }

    public List<Map<String, Object>> listMembers(int guildId) {
        List<Map<String, Object>> members = new ArrayList<>();
        String sql = "SELECT player_uuid, role, joined_at, contributed FROM guild_members WHERE guild_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("player_uuid", rs.getString("player_uuid"));
                    m.put("role", rs.getString("role"));
                    m.put("joined_at", rs.getLong("joined_at"));
                    m.put("contributed", rs.getDouble("contributed"));
                    members.add(m);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error listing guild members", e);
        }
        return members;
    }

    public boolean addMember(int guildId, String playerUuid, String role) {
        String sql = "INSERT OR IGNORE INTO guild_members (guild_id, player_uuid, role, joined_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid);
            stmt.setString(3, role);
            stmt.setLong(4, System.currentTimeMillis());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error adding guild member", e);
        }
        return false;
    }

    public boolean removeMember(int guildId, String playerUuid) {
        String sql = "DELETE FROM guild_members WHERE guild_id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error removing guild member", e);
        }
        return false;
    }
}
