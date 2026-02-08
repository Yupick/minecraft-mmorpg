package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.mobs.MobManager;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.*;

/**
 * Listens for spawn events to control mob spawning
 */
public class SpawnListener implements Listener {
    
    private final MMORPGPlugin plugin;
    private final MobManager mobManager;
    private final Set<EntityType> blockedMobs;
    private final Map<String, Integer> worldMobLimits;
    
    public SpawnListener(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.mobManager = plugin.getMobManager();
        this.blockedMobs = new HashSet<>();
        this.worldMobLimits = new HashMap<>();
        loadConfiguration();
    }
    
    /**
     * Loads spawn configuration
     */
    private void loadConfiguration() {
        // Load blocked mobs from config
        List<String> blocked = plugin.getConfig().getStringList("spawn-control.blocked-mobs");
        for (String mobName : blocked) {
            try {
                EntityType type = EntityType.valueOf(mobName.toUpperCase());
                blockedMobs.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mob type in config: " + mobName);
            }
        }
        
        // Load world mob limits
        if (plugin.getConfig().contains("spawn-control.world-limits")) {
            var section = plugin.getConfig().getConfigurationSection("spawn-control.world-limits");
            if (section != null) {
                for (String world : section.getKeys(false)) {
                    worldMobLimits.put(world, section.getInt(world));
                }
            }
        }
    }
    
    /**
     * Handles creature spawn events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        
        LivingEntity entity = event.getEntity();
        Location location = event.getLocation();
        SpawnReason reason = event.getSpawnReason();
        
        // Check if mob type is blocked
        if (blockedMobs.contains(entity.getType())) {
            event.setCancelled(true);
            return;
        }
        
        // Check world mob limits
        String worldName = location.getWorld().getName();
        if (worldMobLimits.containsKey(worldName)) {
            int limit = worldMobLimits.get(worldName);
            int currentCount = location.getWorld().getLivingEntities().size();
            
            if (currentCount >= limit) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Handle natural spawns vs custom spawns
        if (reason == SpawnReason.NATURAL || reason == SpawnReason.DEFAULT) {
            handleNaturalSpawn(event, entity, location);
        } else if (reason == SpawnReason.CUSTOM || reason == SpawnReason.SPAWNER_EGG) {
            handleCustomSpawn(event, entity, location);
        }
    }
    
    /**
     * Handles natural mob spawns
     */
    private void handleNaturalSpawn(CreatureSpawnEvent event, LivingEntity entity, Location location) {
        // Check if we should replace this spawn with a custom mob
        if (shouldReplaceWithCustomMob(location, entity.getType())) {
            event.setCancelled(true);
            
            // Spawn custom mob instead
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                mobManager.spawnRandomCustomMob(location);
            });
        }
    }
    
    /**
     * Handles custom mob spawns (from spawners, eggs, etc.)
     */
    private void handleCustomSpawn(CreatureSpawnEvent event, LivingEntity entity, Location location) {
        // Allow custom spawns but apply any necessary modifications
        // This could include scaling based on location or world difficulty
    }
    
    /**
     * Determines if a natural spawn should be replaced with a custom mob
     */
    private boolean shouldReplaceWithCustomMob(Location location, EntityType type) {
        // Get replacement chance from config
        double chance = plugin.getConfig().getDouble("spawn-control.custom-mob-chance", 0.15);
        
        // Higher chance in certain biomes or at night
        if (location.getWorld().getTime() > 13000 && location.getWorld().getTime() < 23000) {
            chance *= 1.5; // Night time bonus
        }
        
        // Random roll
        return Math.random() < chance;
    }
    
    /**
     * Handles chunk load events for mob management
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Optional: Pre-spawn custom mobs in loaded chunks
        if (!event.isNewChunk()) return;
        
        boolean shouldPreSpawn = plugin.getConfig().getBoolean("spawn-control.pre-spawn-on-chunk-load", false);
        if (!shouldPreSpawn) return;
        
        // Schedule spawn task
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Spawn a few custom mobs in the new chunk
            int spawnCount = plugin.getConfig().getInt("spawn-control.pre-spawn-count", 2);
            for (int i = 0; i < spawnCount; i++) {
                Location randomLoc = getRandomLocationInChunk(event.getChunk());
                if (randomLoc != null && isValidSpawnLocation(randomLoc)) {
                    mobManager.spawnRandomCustomMob(randomLoc);
                }
            }
        }, 40L); // Wait 2 seconds after chunk load
    }
    
    /**
     * Gets a random valid location within a chunk
     */
    private Location getRandomLocationInChunk(org.bukkit.Chunk chunk) {
        Random random = new Random();
        int x = (chunk.getX() << 4) + random.nextInt(16);
        int z = (chunk.getZ() << 4) + random.nextInt(16);
        int y = chunk.getWorld().getHighestBlockYAt(x, z);
        
        return new Location(chunk.getWorld(), x, y, z);
    }
    
    /**
     * Checks if a location is valid for spawning
     */
    private boolean isValidSpawnLocation(Location location) {
        if (location == null) return false;
        
        // Check if location is in water
        if (location.getBlock().isLiquid()) return false;
        
        // Check if location has air above
        if (!location.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        
        // Check if location is too high or too low
        if (location.getY() < 0 || location.getY() > 250) return false;
        
        return true;
    }
    
    /**
     * Reloads spawn configuration
     */
    public void reload() {
        blockedMobs.clear();
        worldMobLimits.clear();
        loadConfiguration();
        plugin.getLogger().info("Spawn configuration reloaded");
    }
}
