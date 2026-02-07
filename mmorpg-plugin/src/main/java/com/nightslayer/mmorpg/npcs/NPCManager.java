package com.nightslayer.mmorpg.npcs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NPCManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<String, NPC> npcs;
    
    public NPCManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.npcs = new HashMap<>();
        loadNPCs();
    }
    
    private void loadNPCs() {
        String sql = "SELECT * FROM npcs WHERE enabled = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                NPC npc = new NPC(id, name, type);
                npcs.put(id, npc);
            }
            plugin.getLogger().info("Loaded " + npcs.size() + " NPCs");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading NPCs", e);
        }
    }
    
    public NPC getNPC(String id) {
        return npcs.get(id);
    }
    
    public static class NPC {
        private final String id;
        private final String name;
        private final String type;
        
        public NPC(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
    }
}
