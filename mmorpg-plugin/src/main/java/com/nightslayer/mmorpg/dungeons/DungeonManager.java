package com.nightslayer.mmorpg.dungeons;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages dungeons and dungeon instances
 * 
 * Features:
 * - Load dungeons from database
 * - Create dungeon instances for parties
 * - Wave-based combat system
 * - Boss encounters
 * - Rewards distribution
 */
public class DungeonManager {
    
    private final DatabaseManager dbManager;
    private final LanguageManager langManager;
    private final Map<String, Dungeon> dungeons;
    private final Map<UUID, DungeonInstance> activeInstances;
    
    public DungeonManager(DatabaseManager dbManager, LanguageManager langManager) {
        this.dbManager = dbManager;
        this.langManager = langManager;
        this.dungeons = new HashMap<>();
        this.activeInstances = new HashMap<>();
    }
    
    /**
     * Load all dungeons from database
     */
    public void loadDungeons() {
        dungeons.clear();
        String sql = "SELECT * FROM dungeon_definitions";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Dungeon dungeon = new Dungeon(
                    rs.getString("dungeon_id"),
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getDouble("spawn_x"),
                    rs.getDouble("spawn_y"),
                    rs.getDouble("spawn_z"),
                    rs.getInt("min_level"),
                    rs.getInt("max_players"),
                    rs.getInt("waves"),
                    rs.getString("mobs_per_wave"),
                    rs.getString("boss_mob"),
                    rs.getInt("boss_health_multiplier"),
                    rs.getInt("coin_reward"),
                    rs.getInt("exp_reward")
                );
                dungeons.put(dungeon.getDungeonId(), dungeon);
            }
            
            Bukkit.getLogger().info("[MMORPG] Loaded " + dungeons.size() + " dungeons");
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error loading dungeons", e);
        }
    }
    
    /**
     * Get a dungeon by ID
     */
    public Dungeon getDungeon(String dungeonId) {
        return dungeons.get(dungeonId);
    }
    
    /**
     * Create a dungeon instance for a party
     */
    public DungeonInstance createInstance(String dungeonId, List<Player> party) {
        Dungeon dungeon = dungeons.get(dungeonId);
        if (dungeon == null) {
            return null;
        }
        
        // Validate party size
        if (party.size() > dungeon.getMaxPlayers()) {
            return null;
        }
        
        // Validate player levels
        for (Player player : party) {
            int level = getPlayerLevel(player);
            if (level < dungeon.getMinLevel()) {
                player.sendMessage(langManager.getMessage("dungeon.level_too_low", 
                    dungeon.getMinLevel()));
                return null;
            }
        }
        
        // Create instance
        UUID instanceId = UUID.randomUUID();
        DungeonInstance instance = new DungeonInstance(instanceId, dungeon, party);
        activeInstances.put(instanceId, instance);
        
        // Teleport party to dungeon
        Location spawnLoc = dungeon.getSpawnLocation();
        if (spawnLoc != null) {
            for (Player player : party) {
                player.teleport(spawnLoc);
                player.sendMessage(langManager.getMessage("dungeon.entered", dungeon.getName()));
            }
        }
        
        // Start first wave
        instance.startNextWave();
        
        return instance;
    }
    
    /**
     * Get active instance for a player
     */
    public DungeonInstance getPlayerInstance(Player player) {
        for (DungeonInstance instance : activeInstances.values()) {
            if (instance.getParty().contains(player)) {
                return instance;
            }
        }
        return null;
    }
    
    /**
     * Complete a dungeon instance
     */
    public void completeInstance(UUID instanceId) {
        DungeonInstance instance = activeInstances.get(instanceId);
        if (instance == null) return;
        
        Dungeon dungeon = instance.getDungeon();
        
        // Distribute rewards to party
        for (Player player : instance.getParty()) {
            if (player.isOnline()) {
                giveReward(player, dungeon.getCoinReward(), dungeon.getExpReward());
                player.sendMessage(langManager.getMessage("dungeon.completed", dungeon.getName()));
            }
        }
        
        // Remove instance
        activeInstances.remove(instanceId);
    }
    
    /**
     * Fail/abandon a dungeon instance
     */
    public void failInstance(UUID instanceId) {
        DungeonInstance instance = activeInstances.get(instanceId);
        if (instance == null) return;
        
        // Notify party
        for (Player player : instance.getParty()) {
            if (player.isOnline()) {
                player.sendMessage(langManager.getMessage("dungeon.failed"));
            }
        }
        
        // Remove instance
        activeInstances.remove(instanceId);
    }
    
    /**
     * Handle mob death in dungeon
     */
    public void handleMobDeath(LivingEntity entity, Player killer) {
        DungeonInstance instance = getPlayerInstance(killer);
        if (instance == null) return;
        
        instance.onMobKilled(entity);
    }
    
    /**
     * Get player level from database
     */
    private int getPlayerLevel(Player player) {
        String sql = "SELECT level FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error getting player level", e);
        }
        
        return 1;
    }
    
    /**
     * Give rewards to player
     */
    private void giveReward(Player player, int coins, int exp) {
        String updateCoins = "UPDATE player_economy SET coins = coins + ? WHERE uuid = ?";
        String updateExp = "UPDATE players SET experience = experience + ? WHERE uuid = ?";
        
        try (PreparedStatement stmt1 = dbManager.getConnection().prepareStatement(updateCoins);
             PreparedStatement stmt2 = dbManager.getConnection().prepareStatement(updateExp)) {
            
            stmt1.setInt(1, coins);
            stmt1.setString(2, player.getUniqueId().toString());
            stmt1.executeUpdate();
            
            stmt2.setInt(1, exp);
            stmt2.setString(2, player.getUniqueId().toString());
            stmt2.executeUpdate();
            
            player.sendMessage(langManager.getMessage("dungeon.reward", coins, exp));
            
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[MMORPG] Error giving dungeon reward", e);
        }
    }
    
    /**
     * Inner class representing a dungeon definition
     */
    public static class Dungeon {
        private final String dungeonId;
        private final String name;
        private final String worldName;
        private final double spawnX;
        private final double spawnY;
        private final double spawnZ;
        private final int minLevel;
        private final int maxPlayers;
        private final int waves;
        private final String mobsPerWave;
        private final String bossMob;
        private final int bossHealthMultiplier;
        private final int coinReward;
        private final int expReward;
        
        public Dungeon(String dungeonId, String name, String worldName, double spawnX, double spawnY,
                      double spawnZ, int minLevel, int maxPlayers, int waves, String mobsPerWave,
                      String bossMob, int bossHealthMultiplier, int coinReward, int expReward) {
            this.dungeonId = dungeonId;
            this.name = name;
            this.worldName = worldName;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
            this.minLevel = minLevel;
            this.maxPlayers = maxPlayers;
            this.waves = waves;
            this.mobsPerWave = mobsPerWave;
            this.bossMob = bossMob;
            this.bossHealthMultiplier = bossHealthMultiplier;
            this.coinReward = coinReward;
            this.expReward = expReward;
        }
        
        public Location getSpawnLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, spawnX, spawnY, spawnZ);
        }
        
        // Getters
        public String getDungeonId() { return dungeonId; }
        public String getName() { return name; }
        public int getMinLevel() { return minLevel; }
        public int getMaxPlayers() { return maxPlayers; }
        public int getWaves() { return waves; }
        public String getMobsPerWave() { return mobsPerWave; }
        public String getBossMob() { return bossMob; }
        public int getBossHealthMultiplier() { return bossHealthMultiplier; }
        public int getCoinReward() { return coinReward; }
        public int getExpReward() { return expReward; }
    }
    
    /**
     * Inner class representing an active dungeon instance
     */
    public class DungeonInstance {
        private final UUID instanceId;
        private final Dungeon dungeon;
        private final List<Player> party;
        private int currentWave;
        private final Set<UUID> aliveMonsters;
        
        public DungeonInstance(UUID instanceId, Dungeon dungeon, List<Player> party) {
            this.instanceId = instanceId;
            this.dungeon = dungeon;
            this.party = new ArrayList<>(party);
            this.currentWave = 0;
            this.aliveMonsters = new HashSet<>();
        }
        
        /**
         * Start the next wave
         */
        public void startNextWave() {
            currentWave++;
            
            if (currentWave > dungeon.getWaves()) {
                // All waves complete, spawn boss
                spawnBoss();
                return;
            }
            
            // Notify party
            for (Player player : party) {
                if (player.isOnline()) {
                    player.sendMessage(langManager.getMessage("dungeon.wave_start", 
                        currentWave, dungeon.getWaves()));
                }
            }
            
            // Spawn mobs
            spawnWaveMobs();
        }
        
        /**
         * Spawn mobs for current wave
         */
        private void spawnWaveMobs() {
            Location spawnLoc = dungeon.getSpawnLocation();
            if (spawnLoc == null) return;
            
            String[] mobTypes = dungeon.getMobsPerWave().split(",");
            
            for (String mobType : mobTypes) {
                EntityType type = EntityType.valueOf(mobType.trim().toUpperCase());
                
                // Spawn multiple instances based on wave number
                int count = 2 + currentWave;
                
                for (int i = 0; i < count; i++) {
                    Location loc = spawnLoc.clone().add(
                        Math.random() * 10 - 5,
                        0,
                        Math.random() * 10 - 5
                    );
                    
                    LivingEntity entity = (LivingEntity) spawnLoc.getWorld().spawnEntity(loc, type);
                    aliveMonsters.add(entity.getUniqueId());
                }
            }
        }
        
        /**
         * Spawn boss
         */
        private void spawnBoss() {
            Location spawnLoc = dungeon.getSpawnLocation();
            if (spawnLoc == null) return;
            
            // Notify party
            for (Player player : party) {
                if (player.isOnline()) {
                    player.sendMessage(langManager.getMessage("dungeon.boss_spawn"));
                }
            }
            
            // Spawn boss
            EntityType bossType = EntityType.valueOf(dungeon.getBossMob().toUpperCase());
            LivingEntity boss = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, bossType);
            
            // Apply boss multipliers
            boss.setMaxHealth(boss.getMaxHealth() * dungeon.getBossHealthMultiplier());
            boss.setHealth(boss.getMaxHealth());
            boss.setCustomName("§c§l" + dungeon.getName() + " Boss");
            boss.setCustomNameVisible(true);
            
            aliveMonsters.add(boss.getUniqueId());
        }
        
        /**
         * Handle mob death
         */
        public void onMobKilled(LivingEntity entity) {
            aliveMonsters.remove(entity.getUniqueId());
            
            // Check if wave is complete
            if (aliveMonsters.isEmpty()) {
                if (currentWave < dungeon.getWaves()) {
                    // Start next wave after delay
                    Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("MMORPGPlugin"),
                        this::startNextWave,
                        100L // 5 seconds
                    );
                } else {
                    // Dungeon complete
                    completeInstance(instanceId);
                }
            }
        }
        
        // Getters
        public UUID getInstanceId() { return instanceId; }
        public Dungeon getDungeon() { return dungeon; }
        public List<Player> getParty() { return party; }
        public int getCurrentWave() { return currentWave; }
    }
}
