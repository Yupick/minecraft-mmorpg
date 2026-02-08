package com.nightslayer.mmorpg.mobs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class MobManager {
    private final MMORPGPlugin plugin;
    private final DatabaseManager db;
    private final Map<String, CustomMob> customMobs;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    
    public MobManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.customMobs = new HashMap<>();
        loadCustomMobs();
    }
    
    private void loadCustomMobs() {
        String sql = "SELECT * FROM custom_mobs WHERE enabled = 1";
        try (ResultSet rs = db.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String entityType = rs.getString("type");
                String displayName = rs.getString("name");
                int level = rs.getInt("level");
                double health = rs.getDouble("health");
                double damage = rs.getDouble("damage");
                
                CustomMob mob = new CustomMob(id, entityType, displayName, level, health, damage);
                customMobs.put(id, mob);
            }
            plugin.getLogger().info("Loaded " + customMobs.size() + " custom mobs");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading custom mobs", e);
        }
    }
    
    public void applyCustomMob(LivingEntity entity, String mobId) {
        CustomMob mob = customMobs.get(mobId);
        if (mob != null) {
            entity.customName(LEGACY.deserialize(mob.displayName));
            entity.setCustomNameVisible(true);
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mob.health);
            entity.setHealth(mob.health);
            if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(mob.damage);
            }
        }
    }

    public void spawnRandomCustomMob(Location location) {
        if (customMobs.isEmpty() || location == null || location.getWorld() == null) {
            return;
        }
        List<CustomMob> mobs = new ArrayList<>(customMobs.values());
        CustomMob mob = mobs.get(new Random().nextInt(mobs.size()));
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mob.entityType.toUpperCase());
        } catch (Exception e) {
            return;
        }
        if (!entityType.isSpawnable() || !entityType.isAlive()) {
            return;
        }
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);
        applyCustomMob(entity, mob.id);
    }
    
    public static class CustomMob {
        private final String id;
        private final String entityType;
        private final String displayName;
        private final int level;
        private final double health;
        private final double damage;
        
        public CustomMob(String id, String entityType, String displayName, int level, double health, double damage) {
            this.id = id;
            this.entityType = entityType;
            this.displayName = displayName;
            this.level = level;
            this.health = health;
            this.damage = damage;
        }
        
        public String getId() { return id; }
        public int getLevel() { return level; }
        public double getDamage() { return damage; }
    }
}
