package com.nightslayer.mmorpg;

import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.database.DatabaseMigration;
import com.nightslayer.mmorpg.database.WorldDatabaseManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for Minecraft MMORPG System.
 */
public class MMORPGPlugin extends JavaPlugin {
    
    private static MMORPGPlugin instance;
    
    // Core components
    private DatabaseManager databaseManager;
    private WorldDatabaseManager worldDatabaseManager;
    private LanguageManager languageManager;
    
    // Phase 3 managers
    private com.nightslayer.mmorpg.npcs.NPCManager npcManager;
    private com.nightslayer.mmorpg.quests.QuestManager questManager;
    private com.nightslayer.mmorpg.items.ItemManager itemManager;
    private com.nightslayer.mmorpg.mobs.MobManager mobManager;
    private com.nightslayer.mmorpg.economy.EconomyManager economyManager;
    
    // Phase 4 managers
    private com.nightslayer.mmorpg.crafting.CraftingManager craftingManager;
    private com.nightslayer.mmorpg.enchanting.EnchantmentManager enchantmentManager;
    private com.nightslayer.mmorpg.respawn.RespawnManager respawnManager;
    private com.nightslayer.mmorpg.dungeons.DungeonManager dungeonManager;
    private com.nightslayer.mmorpg.invasions.InvasionManager invasionManager;
    private com.nightslayer.mmorpg.pets.PetManager petManager;
    private com.nightslayer.mmorpg.spawns.SpawnManager spawnManager;
    private com.nightslayer.mmorpg.ranks.RankManager rankManager;
    private com.nightslayer.mmorpg.achievements.AchievementManager achievementManager;
    private com.nightslayer.mmorpg.bestiary.BestiaryManager bestiaryManager;
    private com.nightslayer.mmorpg.api.RPGAdminAPI adminAPI;

    // Post-launch managers
    private com.nightslayer.mmorpg.social.GuildManager guildManager;
    private com.nightslayer.mmorpg.social.FriendsManager friendsManager;
    private com.nightslayer.mmorpg.social.PrivateMessageManager privateMessageManager;
    private com.nightslayer.mmorpg.social.MailManager mailManager;
    private com.nightslayer.mmorpg.professions.ProfessionManager professionManager;
    private com.nightslayer.mmorpg.pvp.PvpManager pvpManager;
    private com.nightslayer.mmorpg.events.SeasonalEventManager seasonalEventManager;
    private com.nightslayer.mmorpg.integration.IntegrationManager integrationManager;
    private com.nightslayer.mmorpg.optimization.OptimizationManager optimizationManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  Minecraft MMORPG System");
        getLogger().info("  Version: " + getDescription().getVersion());
        getLogger().info("  Author: NightSlayer");
        getLogger().info("═══════════════════════════════════════");
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize language manager
        getLogger().info("Loading language files...");
        languageManager = new LanguageManager(this);
        languageManager.loadLanguages();
        
        // Initialize database
        getLogger().info("Initializing database connection...");
        String databasePath = getConfig().getString("database.path", "config/data/universal.db");
        databaseManager = DatabaseManager.getInstance(this, databasePath);
        databaseManager.initializeConnection();
        
        // Run database migrations
        getLogger().info("Running database migrations...");
        try {
            DatabaseMigration migration = new DatabaseMigration(this);
            migration.createTables();
            migration.migrateAllConfigs();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Database migration failed!", e);
        }
        
        // Initialize world database manager
        getLogger().info("Initializing world database manager...");
        worldDatabaseManager = new WorldDatabaseManager(this);
        worldDatabaseManager.initializeWorldDatabase();
        
        // Initialize managers (Phase 3)
        getLogger().info("Initializing game managers...");
        initializeManagers();
        
        // Register event listeners (Phase 3)
        getLogger().info("Registering event listeners...");
        registerListeners();
        
        // Register commands (Phase 3)
        getLogger().info("Registering commands...");
        registerCommands();
        
        // TODO: Start scheduled tasks (Phase 4)
        // startTasks();
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  Plugin enabled successfully!");
        getLogger().info("  Database: " + (databaseManager.isConnected() ? "Connected" : "Disconnected"));
        getLogger().info("  Language: " + languageManager.getCurrentLanguage());
        getLogger().info("═══════════════════════════════════════");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  Shutting down Minecraft MMORPG System");
        getLogger().info("═══════════════════════════════════════");
        
        // TODO: Save player data
        // saveAllPlayerData();
        
        // TODO: Stop scheduled tasks
        // stopTasks();
        
        // TODO: Save all manager data
        // saveAllData();
        
        // Close database connections
        if (databaseManager != null) {
            getLogger().info("Closing database connections...");
            databaseManager.closeConnection();
        }
        
        if (worldDatabaseManager != null) {
            worldDatabaseManager.closeAllConnections();
        }
        
        getLogger().info("Plugin disabled successfully!");
        instance = null;
    }
    
    /**
     * Initialize all game managers.
     */
    private void initializeManagers() {
        // Phase 3: Basic managers
        npcManager = new com.nightslayer.mmorpg.npcs.NPCManager(this);
        questManager = new com.nightslayer.mmorpg.quests.QuestManager(this);
        itemManager = new com.nightslayer.mmorpg.items.ItemManager(this);
        mobManager = new com.nightslayer.mmorpg.mobs.MobManager(this);
        economyManager = new com.nightslayer.mmorpg.economy.EconomyManager(this);
        
        // Phase 4: Advanced managers
        getLogger().info("Initializing advanced managers...");
        craftingManager = new com.nightslayer.mmorpg.crafting.CraftingManager(databaseManager, languageManager);
        enchantmentManager = new com.nightslayer.mmorpg.enchanting.EnchantmentManager(databaseManager, languageManager);
        respawnManager = new com.nightslayer.mmorpg.respawn.RespawnManager(databaseManager);
        dungeonManager = new com.nightslayer.mmorpg.dungeons.DungeonManager(databaseManager, languageManager);
        invasionManager = new com.nightslayer.mmorpg.invasions.InvasionManager(databaseManager);
        petManager = new com.nightslayer.mmorpg.pets.PetManager(databaseManager, languageManager);
        spawnManager = new com.nightslayer.mmorpg.spawns.SpawnManager(databaseManager);
        rankManager = new com.nightslayer.mmorpg.ranks.RankManager(databaseManager, languageManager);
        achievementManager = new com.nightslayer.mmorpg.achievements.AchievementManager(databaseManager, languageManager);
        bestiaryManager = new com.nightslayer.mmorpg.bestiary.BestiaryManager(databaseManager, languageManager);
        adminAPI = new com.nightslayer.mmorpg.api.RPGAdminAPI(databaseManager);

        // Post-launch managers
        guildManager = new com.nightslayer.mmorpg.social.GuildManager(databaseManager);
        friendsManager = new com.nightslayer.mmorpg.social.FriendsManager(databaseManager);
        privateMessageManager = new com.nightslayer.mmorpg.social.PrivateMessageManager(databaseManager);
        mailManager = new com.nightslayer.mmorpg.social.MailManager(databaseManager);
        professionManager = new com.nightslayer.mmorpg.professions.ProfessionManager(databaseManager);
        pvpManager = new com.nightslayer.mmorpg.pvp.PvpManager(databaseManager);
        seasonalEventManager = new com.nightslayer.mmorpg.events.SeasonalEventManager(databaseManager);
        integrationManager = new com.nightslayer.mmorpg.integration.IntegrationManager(databaseManager);
        optimizationManager = new com.nightslayer.mmorpg.optimization.OptimizationManager(databaseManager);
        
        getLogger().info("All managers initialized successfully!");
    }
    
    /**
     * Register event listeners.
     */
    private void registerListeners() {
        // Phase 3: Basic listeners
        getServer().getPluginManager().registerEvents(new com.nightslayer.mmorpg.listeners.MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.nightslayer.mmorpg.listeners.PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.nightslayer.mmorpg.listeners.SpawnListener(this), this);
        
        // Phase 4: Advanced listeners (TODO)
        getServer().getPluginManager().registerEvents(new com.nightslayer.mmorpg.crafting.CraftingGUI(this), this);
        getServer().getPluginManager().registerEvents(new com.nightslayer.mmorpg.enchanting.EnchantingGUI(this), this);
        // getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        // getServer().getPluginManager().registerEvents(new PetListener(this), this);
        // getServer().getPluginManager().registerEvents(new SquadListener(this), this);
    }
    
    /**
     * Register commands.
     */
    private void registerCommands() {
        // Phase 3: Basic commands
        getCommand("class").setExecutor(new com.nightslayer.mmorpg.commands.ClassCommand(this));
        getCommand("stats").setExecutor(new com.nightslayer.mmorpg.commands.StatsCommand(this));
        getCommand("balance").setExecutor(new com.nightslayer.mmorpg.commands.BalanceCommand(this));
        
        // Phase 4: Advanced commands (TODO)
        com.nightslayer.mmorpg.commands.QuestCommand questCommand = new com.nightslayer.mmorpg.commands.QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);
        // getCommand("pay").setExecutor(new PayCommand(this));
        // getCommand("squad").setExecutor(new SquadCommand(this));
        // getCommand("pets").setExecutor(new PetsCommand(this));
        // getCommand("bestiary").setExecutor(new BestiaryCommand(this));
        // getCommand("achievements").setExecutor(new AchievementsCommand(this));
        // getCommand("mmorpgadmin").setExecutor(new AdminCommand(this));
    }
    
    /**
     * Start scheduled tasks.
     * TODO: Implement in Phase 4
     */
    private void startTasks() {
        getLogger().info("Starting scheduled tasks...");
        
        // TODO: Auto-save task every 5 minutes
        // TODO: Spawn manager tick task
        // TODO: Invasion check task
        // TODO: Dungeon manager task
        // TODO: Pet AI task
    }
    
    // Getters for managers
    
    public static MMORPGPlugin getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public WorldDatabaseManager getWorldDatabaseManager() {
        return worldDatabaseManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public com.nightslayer.mmorpg.economy.EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public com.nightslayer.mmorpg.npcs.NPCManager getNPCManager() {
        return npcManager;
    }
    
    public com.nightslayer.mmorpg.quests.QuestManager getQuestManager() {
        return questManager;
    }
    
    public com.nightslayer.mmorpg.items.ItemManager getItemManager() {
        return itemManager;
    }
    
    public com.nightslayer.mmorpg.mobs.MobManager getMobManager() {
        return mobManager;
    }
    
    // Phase 4 managers getters
    
    public com.nightslayer.mmorpg.crafting.CraftingManager getCraftingManager() {
        return craftingManager;
    }
    
    public com.nightslayer.mmorpg.enchanting.EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public com.nightslayer.mmorpg.social.GuildManager getGuildManager() {
        return guildManager;
    }

    public com.nightslayer.mmorpg.social.FriendsManager getFriendsManager() {
        return friendsManager;
    }

    public com.nightslayer.mmorpg.social.PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public com.nightslayer.mmorpg.social.MailManager getMailManager() {
        return mailManager;
    }

    public com.nightslayer.mmorpg.professions.ProfessionManager getProfessionManager() {
        return professionManager;
    }

    public com.nightslayer.mmorpg.pvp.PvpManager getPvpManager() {
        return pvpManager;
    }

    public com.nightslayer.mmorpg.events.SeasonalEventManager getSeasonalEventManager() {
        return seasonalEventManager;
    }

    public com.nightslayer.mmorpg.integration.IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public com.nightslayer.mmorpg.optimization.OptimizationManager getOptimizationManager() {
        return optimizationManager;
    }
    
    public com.nightslayer.mmorpg.respawn.RespawnManager getRespawnManager() {
        return respawnManager;
    }
    
    public com.nightslayer.mmorpg.dungeons.DungeonManager getDungeonManager() {
        return dungeonManager;
    }
    
    public com.nightslayer.mmorpg.invasions.InvasionManager getInvasionManager() {
        return invasionManager;
    }
    
    public com.nightslayer.mmorpg.pets.PetManager getPetManager() {
        return petManager;
    }
    
    public com.nightslayer.mmorpg.spawns.SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    public com.nightslayer.mmorpg.ranks.RankManager getRankManager() {
        return rankManager;
    }
    
    public com.nightslayer.mmorpg.achievements.AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public com.nightslayer.mmorpg.bestiary.BestiaryManager getBestiaryManager() {
        return bestiaryManager;
    }
    
    public com.nightslayer.mmorpg.api.RPGAdminAPI getAdminAPI() {
        return adminAPI;
    }
}
