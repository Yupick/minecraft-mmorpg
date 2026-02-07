package com.nightslayer.mmorpg.quests;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.models.Quest;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class QuestManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<Integer, Quest> quests;
    
    public QuestManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.quests = new HashMap<>();
        loadQuests();
    }
    
    private void loadQuests() {
        String sql = "SELECT * FROM quests WHERE active = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                int minLevel = rs.getInt("min_level");
                String type = rs.getString("type");
                int coinReward = rs.getInt("coin_reward");
                int expReward = rs.getInt("exp_reward");
                
                Quest quest = new Quest(id, name, description, minLevel, type, coinReward, expReward);
                quests.put(id, quest);
            }
            plugin.getLogger().info("Loaded " + quests.size() + " quests");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading quests", e);
        }
    }
    
    public Quest getQuest(int id) {
        return quests.get(id);
    }

    public Quest getQuestById(int id) {
        return getQuest(id);
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }
    
    public boolean acceptQuest(UUID playerId, int questId) {
        String sql = "INSERT INTO player_quests (player_uuid, quest_id, status, progress) VALUES (?, ?, 'active', 0)";
        return db.executeUpdate(sql, playerId.toString(), questId) > 0;
    }
    
    public boolean completeQuest(UUID playerId, int questId) {
        String sql = "UPDATE player_quests SET status = 'completed', completed_at = CURRENT_TIMESTAMP WHERE player_uuid = ? AND quest_id = ?";
        return db.executeUpdate(sql, playerId.toString(), questId) > 0;
    }

    public boolean startQuest(Player player, int questId) {
        return acceptQuest(player.getUniqueId(), questId);
    }

    public boolean abandonQuest(UUID playerId, int questId) {
        String sql = "DELETE FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        return db.executeUpdate(sql, playerId.toString(), questId) > 0;
    }

    public List<Quest> getActiveQuests(UUID playerId) {
        List<Quest> active = new ArrayList<>();
        String sql = "SELECT quest_id FROM player_quests WHERE player_uuid = ? AND status = 'active'";
        try (ResultSet rs = db.executeQuery(sql, playerId.toString())) {
            while (rs != null && rs.next()) {
                int questId = rs.getInt("quest_id");
                Quest quest = quests.get(questId);
                if (quest != null) {
                    active.add(quest);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading active quests", e);
        }
        return active;
    }

    public int getQuestProgress(UUID playerId, int questId) {
        String sql = "SELECT progress FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        try (ResultSet rs = db.executeQuery(sql, playerId.toString(), questId)) {
            if (rs != null && rs.next()) {
                return rs.getInt("progress");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading quest progress", e);
        }
        return 0;
    }

    public List<Quest> getAvailableQuests(Player player) {
        int playerLevel = getPlayerLevel(player.getUniqueId());
        List<Quest> available = new ArrayList<>();
        List<Quest> active = getActiveQuests(player.getUniqueId());
        
        for (Quest quest : quests.values()) {
            if (quest.getMinLevel() <= playerLevel && !active.contains(quest)) {
                available.add(quest);
            }
        }
        
        return available;
    }

    private int getPlayerLevel(UUID playerId) {
        String sql = "SELECT level FROM players WHERE uuid = ?";
        try (ResultSet rs = db.executeQuery(sql, playerId.toString())) {
            if (rs != null && rs.next()) {
                return rs.getInt("level");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player level", e);
        }
        return 1;
    }
}
