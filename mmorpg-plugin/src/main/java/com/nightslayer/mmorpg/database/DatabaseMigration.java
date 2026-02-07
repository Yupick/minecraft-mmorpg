package com.nightslayer.mmorpg.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.MMORPGPlugin;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles database migration from JSON files to SQLite.
 * 
 * CRITICAL WARNING:
 * - NEVER close the Connection obtained from DatabaseManager (it's a singleton!)
 * - ONLY use try-with-resources for PreparedStatement and ResultSet
 * - Always call conn.commit() after batch operations
 */
public class DatabaseMigration {
    
    private final MMORPGPlugin plugin;
    private final DatabaseManager dbManager;
    private final Gson gson;
    
    public DatabaseMigration(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new Gson();
    }
    
    /**
     * Run all database migrations.
     */
    public void migrate() {
        plugin.getLogger().info("Starting database migration...");
        
        try {
            createTables();
            migrateConfigData();
            generateDefaultData();
            
            plugin.getLogger().info("Database migration completed successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database migration failed!", e);
        }
    }
    
    /**
     * Create all database tables.
     */
    public void createTables() throws SQLException {
        plugin.getLogger().info("Creating database tables...");
        
        Connection conn = dbManager.getConnection();
        
        // Players table
        String playersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                class_type TEXT DEFAULT 'warrior',
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                health REAL DEFAULT 20.0,
                max_health REAL DEFAULT 20.0,
                mana REAL DEFAULT 100.0,
                max_mana REAL DEFAULT 100.0,
                created_at INTEGER,
                last_login INTEGER
            )
            """;
        
        // Player abilities table
        String abilitiesTable = """
            CREATE TABLE IF NOT EXISTS player_abilities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                ability_id TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;
        
        // Player quests table
        String questsTable = """
            CREATE TABLE IF NOT EXISTS player_quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                quest_id TEXT NOT NULL,
                status TEXT DEFAULT 'active',
                progress INTEGER DEFAULT 0,
                started_at INTEGER,
                completed_at INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;
        
        // Player economy table
        String economyTable = """
            CREATE TABLE IF NOT EXISTS player_economy (
                player_uuid TEXT PRIMARY KEY,
                balance REAL DEFAULT 0.0,
                total_earned REAL DEFAULT 0.0,
                total_spent REAL DEFAULT 0.0,
                last_updated INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;
        
        // NPCs table
        String npcsTable = """
            CREATE TABLE IF NOT EXISTS npcs (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT,
                world TEXT,
                x REAL,
                y REAL,
                z REAL,
                yaw REAL,
                pitch REAL,
                dialogues_json TEXT,
                trades_json TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Quests definitions table
        String questsDefTable = """
            CREATE TABLE IF NOT EXISTS quests (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                difficulty TEXT,
                min_level INTEGER DEFAULT 1,
                max_level INTEGER,
                objectives_json TEXT,
                rewards_json TEXT,
                requirements_json TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Crafting recipes table
        String craftingTable = """
            CREATE TABLE IF NOT EXISTS crafting_recipes (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                required_level INTEGER DEFAULT 1,
                ingredients TEXT,
                result TEXT,
                cost_coins INTEGER DEFAULT 0,
                cost_xp INTEGER DEFAULT 0,
                success_rate INTEGER DEFAULT 100,
                category TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Enchantments table
        String enchantmentsTable = """
            CREATE TABLE IF NOT EXISTS enchantments (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                type TEXT,
                max_level INTEGER DEFAULT 1,
                base_cost INTEGER DEFAULT 100,
                cost_per_level INTEGER DEFAULT 50,
                rarity TEXT,
                applicable_items TEXT,
                incompatible_with TEXT,
                effects TEXT,
                min_level_required INTEGER DEFAULT 1,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Custom mobs table
        String mobsTable = """
            CREATE TABLE IF NOT EXISTS custom_mobs (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT,
                health REAL DEFAULT 20.0,
                damage REAL DEFAULT 1.0,
                level INTEGER DEFAULT 1,
                drops_json TEXT,
                abilities_json TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Dungeons table
        String dungeonsTable = """
            CREATE TABLE IF NOT EXISTS dungeon_definitions (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                difficulty TEXT,
                min_level INTEGER,
                max_level INTEGER,
                min_players INTEGER DEFAULT 1,
                max_players INTEGER DEFAULT 5,
                time_limit_minutes INTEGER DEFAULT 30,
                entrance_world TEXT,
                entrance_x REAL,
                entrance_y REAL,
                entrance_z REAL,
                waves_json TEXT,
                rewards_json TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Invasions table
        String invasionsTable = """
            CREATE TABLE IF NOT EXISTS invasions (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                type TEXT,
                enabled INTEGER DEFAULT 1,
                trigger_type TEXT,
                interval_hours INTEGER,
                duration_minutes INTEGER,
                waves_json TEXT,
                rewards_json TEXT,
                announcement_json TEXT
            )
            """;
        
        // Pets table
        String petsTable = """
            CREATE TABLE IF NOT EXISTS pets (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                type TEXT,
                rarity TEXT,
                base_health REAL,
                base_damage REAL,
                can_be_mount INTEGER DEFAULT 0,
                mount_speed REAL,
                min_level_required INTEGER DEFAULT 1,
                adoption_cost INTEGER,
                stats_json TEXT,
                abilities_json TEXT,
                food_preference TEXT,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Player pets table
        String playerPetsTable = """
            CREATE TABLE IF NOT EXISTS player_pets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                pet_id TEXT NOT NULL,
                custom_name TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                health REAL,
                is_active INTEGER DEFAULT 0,
                adopted_at INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (pet_id) REFERENCES pets(id)
            )
            """;
        
        // Achievements table
        String achievementsTable = """
            CREATE TABLE IF NOT EXISTS achievements_definitions (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                category TEXT,
                points INTEGER DEFAULT 0,
                requirements_json TEXT,
                rewards_json TEXT,
                hidden INTEGER DEFAULT 0,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Player achievements table
        String playerAchievementsTable = """
            CREATE TABLE IF NOT EXISTS player_achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                achievement_id TEXT NOT NULL,
                progress INTEGER DEFAULT 0,
                unlocked INTEGER DEFAULT 0,
                unlocked_at INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (achievement_id) REFERENCES achievements_definitions(id)
            )
            """;
        
        // Respawn zones table
        String respawnTable = """
            CREATE TABLE IF NOT EXISTS respawn_zones (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                world TEXT,
                x REAL,
                y REAL,
                z REAL,
                yaw REAL,
                pitch REAL,
                radius REAL,
                is_default INTEGER DEFAULT 0,
                require_permission INTEGER DEFAULT 0,
                permission TEXT,
                min_level INTEGER,
                class_requirement TEXT,
                priority INTEGER DEFAULT 50,
                effects_json TEXT,
                invulnerability_duration INTEGER,
                enabled INTEGER DEFAULT 1
            )
            """;
        
        // Squads table
        String squadsTable = """
            CREATE TABLE IF NOT EXISTS squads (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                tag TEXT,
                leader_uuid TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                created_at INTEGER,
                bank_balance REAL DEFAULT 0.0,
                max_members INTEGER DEFAULT 10,
                home_world TEXT,
                home_x REAL,
                home_y REAL,
                home_z REAL,
                FOREIGN KEY (leader_uuid) REFERENCES players(uuid)
            )
            """;
        
        // Squad members table
        String squadMembersTable = """
            CREATE TABLE IF NOT EXISTS squad_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                squad_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                rank TEXT DEFAULT 'recruit',
                joined_at INTEGER,
                contribution_points INTEGER DEFAULT 0,
                FOREIGN KEY (squad_id) REFERENCES squads(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(squad_id, player_uuid)
            )
            """;
        
        // Transactions table
        String transactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                balance_after REAL,
                description TEXT,
                timestamp INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;
        
        // Admin users table (for web panel)
        String adminUsersTable = """
            CREATE TABLE IF NOT EXISTS admin_users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at INTEGER,
                last_login INTEGER,
                is_active INTEGER DEFAULT 1
            )
            """;
        
        // System logs table
        String logsTable = """
            CREATE TABLE IF NOT EXISTS system_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT,
                category TEXT,
                message TEXT,
                timestamp INTEGER
            )
            """;

        // ===== Post-launch: Social Systems =====
        String guildsTable = """
            CREATE TABLE IF NOT EXISTS guilds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                tag TEXT UNIQUE,
                leader_uuid TEXT NOT NULL,
                description TEXT,
                created_at INTEGER,
                max_members INTEGER DEFAULT 20,
                bank_balance REAL DEFAULT 0.0,
                FOREIGN KEY (leader_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;

        String guildMembersTable = """
            CREATE TABLE IF NOT EXISTS guild_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                role TEXT DEFAULT 'member',
                joined_at INTEGER,
                contributed REAL DEFAULT 0.0,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(guild_id, player_uuid)
            )
            """;

        String friendsTable = """
            CREATE TABLE IF NOT EXISTS friends (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                friend_uuid TEXT NOT NULL,
                status TEXT DEFAULT 'pending',
                created_at INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (friend_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(player_uuid, friend_uuid)
            )
            """;

        String privateMessagesTable = """
            CREATE TABLE IF NOT EXISTS private_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_uuid TEXT NOT NULL,
                receiver_uuid TEXT NOT NULL,
                content TEXT NOT NULL,
                sent_at INTEGER,
                is_read INTEGER DEFAULT 0,
                FOREIGN KEY (sender_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (receiver_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;

        String mailTable = """
            CREATE TABLE IF NOT EXISTS mail_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_uuid TEXT,
                receiver_uuid TEXT NOT NULL,
                subject TEXT,
                content TEXT NOT NULL,
                sent_at INTEGER,
                is_read INTEGER DEFAULT 0,
                claimed_rewards INTEGER DEFAULT 0,
                FOREIGN KEY (sender_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                FOREIGN KEY (receiver_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;

        // ===== Post-launch: Professions =====
        String professionsTable = """
            CREATE TABLE IF NOT EXISTS professions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                description TEXT,
                max_level INTEGER DEFAULT 100
            )
            """;

        String playerProfessionsTable = """
            CREATE TABLE IF NOT EXISTS player_professions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                profession_id INTEGER NOT NULL,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                last_updated INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (profession_id) REFERENCES professions(id) ON DELETE CASCADE,
                UNIQUE(player_uuid, profession_id)
            )
            """;

        String professionRecipesTable = """
            CREATE TABLE IF NOT EXISTS profession_recipes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                profession_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                result_item TEXT NOT NULL,
                materials TEXT NOT NULL,
                level_required INTEGER DEFAULT 1,
                FOREIGN KEY (profession_id) REFERENCES professions(id) ON DELETE CASCADE
            )
            """;

        // ===== Post-launch: PvP =====
        String pvpArenasTable = """
            CREATE TABLE IF NOT EXISTS pvp_arenas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                world TEXT NOT NULL,
                x REAL,
                y REAL,
                z REAL,
                radius REAL DEFAULT 25.0,
                is_active INTEGER DEFAULT 1
            )
            """;

        String pvpMatchesTable = """
            CREATE TABLE IF NOT EXISTS pvp_matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                arena_id INTEGER,
                player_a TEXT NOT NULL,
                player_b TEXT NOT NULL,
                winner_uuid TEXT,
                started_at INTEGER,
                ended_at INTEGER,
                FOREIGN KEY (arena_id) REFERENCES pvp_arenas(id) ON DELETE SET NULL,
                FOREIGN KEY (player_a) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (player_b) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (winner_uuid) REFERENCES players(uuid) ON DELETE SET NULL
            )
            """;

        String pvpRankingsTable = """
            CREATE TABLE IF NOT EXISTS pvp_rankings (
                player_uuid TEXT PRIMARY KEY,
                rating INTEGER DEFAULT 1000,
                wins INTEGER DEFAULT 0,
                losses INTEGER DEFAULT 0,
                last_match INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """;

        String tournamentsTable = """
            CREATE TABLE IF NOT EXISTS tournaments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                status TEXT DEFAULT 'scheduled',
                max_players INTEGER DEFAULT 16,
                started_at INTEGER,
                ended_at INTEGER
            )
            """;

        String tournamentParticipantsTable = """
            CREATE TABLE IF NOT EXISTS tournament_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tournament_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                status TEXT DEFAULT 'registered',
                joined_at INTEGER,
                FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(tournament_id, player_uuid)
            )
            """;

        // ===== Post-launch: Events =====
        String eventsTable = """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT DEFAULT 'scheduled',
                start_time INTEGER,
                end_time INTEGER,
                config_json TEXT
            )
            """;

        String eventParticipantsTable = """
            CREATE TABLE IF NOT EXISTS event_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                progress INTEGER DEFAULT 0,
                reward_claimed INTEGER DEFAULT 0,
                FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                UNIQUE(event_id, player_uuid)
            )
            """;

        // ===== Post-launch: Integrations =====
        String webhooksTable = """
            CREATE TABLE IF NOT EXISTS integrations_webhooks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                secret TEXT,
                events TEXT,
                is_active INTEGER DEFAULT 1,
                created_at INTEGER
            )
            """;

        // ===== Post-launch: Optimization & Monitoring =====
        String metricsTable = """
            CREATE TABLE IF NOT EXISTS metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT NOT NULL,
                value REAL,
                timestamp INTEGER
            )
            """;

        String backupsTable = """
            CREATE TABLE IF NOT EXISTS backups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                path TEXT NOT NULL,
                size INTEGER DEFAULT 0,
                created_at INTEGER,
                status TEXT DEFAULT 'created'
            )
            """;
        
        // Execute all CREATE TABLE statements
        List<String> tables = List.of(
            playersTable, abilitiesTable, questsTable, economyTable,
            npcsTable, questsDefTable, craftingTable, enchantmentsTable,
            mobsTable, dungeonsTable, invasionsTable, petsTable,
            playerPetsTable, achievementsTable, playerAchievementsTable,
            respawnTable, squadsTable, squadMembersTable, transactionsTable,
            adminUsersTable, logsTable,
            guildsTable, guildMembersTable, friendsTable, privateMessagesTable, mailTable,
            professionsTable, playerProfessionsTable, professionRecipesTable,
            pvpArenasTable, pvpMatchesTable, pvpRankingsTable, tournamentsTable, tournamentParticipantsTable,
            eventsTable, eventParticipantsTable,
            webhooksTable, metricsTable, backupsTable
        );
        
        for (String tableSql : tables) {
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                stmt.execute();
            }
        }
        
        plugin.getLogger().info("Database tables created successfully!");
    }
    
    /**
     * Migrate data from JSON config files to database.
     */
    public void migrateAllConfigs() {
        plugin.getLogger().info("Migrating configuration data...");
        
        File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
        File configDir = new File(serverRoot, "config");
        
        migrateCraftingRecipes(new File(configDir, "crafting_config.json"));
        migrateEnchantments(new File(configDir, "enchanting_config.json"));
        migrateRespawnZones(new File(configDir, "respawn_config.json"));
        migrateDungeons(new File(configDir, "dungeon_config.json"));
        migrateInvasions(new File(configDir, "events_config.json"));
        migratePets(new File(configDir, "pets_config.json"));
    }
    
    /**
     * Migrate data from JSON config files to database (private version for runMigration).
     */
    private void migrateConfigData() {
        migrateAllConfigs();
    }
    
    /**
     * Migrate crafting recipes from JSON.
     */
    private void migrateCraftingRecipes(File file) {
        if (!file.exists()) {
            plugin.getLogger().warning("Crafting config not found: " + file.getPath());
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray recipes = root.getAsJsonArray("recipes");
            
            if (recipes == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO crafting_recipes 
                (id, name, description, required_level, ingredients, result, cost_coins, cost_xp, success_rate, category)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < recipes.size(); i++) {
                JsonObject recipe = recipes.get(i).getAsJsonObject();
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, recipe.get("id").getAsString());
                    stmt.setString(2, recipe.get("name").getAsString());
                    stmt.setString(3, recipe.has("description") ? recipe.get("description").getAsString() : "");
                    stmt.setInt(4, recipe.get("required_level").getAsInt());
                    stmt.setString(5, recipe.get("ingredients").toString());
                    stmt.setString(6, recipe.get("result").toString());
                    stmt.setInt(7, recipe.get("cost_coins").getAsInt());
                    stmt.setInt(8, recipe.get("cost_xp").getAsInt());
                    stmt.setInt(9, recipe.get("success_rate").getAsInt());
                    stmt.setString(10, recipe.has("category") ? recipe.get("category").getAsString() : "other");
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + recipes.size() + " crafting recipes");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate crafting recipes", e);
        }
    }
    
    /**
     * Migrate enchantments from JSON.
     */
    private void migrateEnchantments(File file) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray enchantments = root.getAsJsonArray("enchantments");
            
            if (enchantments == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO enchantments 
                (id, name, description, type, max_level, base_cost, cost_per_level, rarity, 
                 applicable_items, incompatible_with, effects, min_level_required)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < enchantments.size(); i++) {
                JsonObject ench = enchantments.get(i).getAsJsonObject();
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, ench.get("id").getAsString());
                    stmt.setString(2, ench.get("name").getAsString());
                    stmt.setString(3, ench.get("description").getAsString());
                    stmt.setString(4, ench.get("type").getAsString());
                    stmt.setInt(5, ench.get("max_level").getAsInt());
                    stmt.setInt(6, ench.get("base_cost").getAsInt());
                    stmt.setInt(7, ench.get("cost_per_level").getAsInt());
                    stmt.setString(8, ench.get("rarity").getAsString());
                    stmt.setString(9, ench.get("applicable_items").toString());
                    stmt.setString(10, ench.get("incompatible_with").toString());
                    stmt.setString(11, ench.get("effects").toString());
                    stmt.setInt(12, ench.get("min_level_required").getAsInt());
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + enchantments.size() + " enchantments");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate enchantments", e);
        }
    }
    
    /**
     * Migrate respawn zones from JSON.
     */
    private void migrateRespawnZones(File file) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray zones = root.getAsJsonArray("respawn_zones");
            
            if (zones == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO respawn_zones 
                (id, name, description, world, x, y, z, yaw, pitch, radius, is_default, 
                 require_permission, permission, min_level, class_requirement, priority, 
                 effects_json, invulnerability_duration)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < zones.size(); i++) {
                JsonObject zone = zones.get(i).getAsJsonObject();
                JsonObject loc = zone.getAsJsonObject("location");
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, zone.get("id").getAsString());
                    stmt.setString(2, zone.get("name").getAsString());
                    stmt.setString(3, zone.get("description").getAsString());
                    stmt.setString(4, zone.get("world").getAsString());
                    stmt.setDouble(5, loc.get("x").getAsDouble());
                    stmt.setDouble(6, loc.get("y").getAsDouble());
                    stmt.setDouble(7, loc.get("z").getAsDouble());
                    stmt.setDouble(8, loc.get("yaw").getAsDouble());
                    stmt.setDouble(9, loc.get("pitch").getAsDouble());
                    stmt.setDouble(10, zone.get("radius").getAsDouble());
                    stmt.setInt(11, zone.get("is_default").getAsBoolean() ? 1 : 0);
                    stmt.setInt(12, zone.get("require_permission").getAsBoolean() ? 1 : 0);
                    stmt.setString(13, zone.has("permission") ? zone.get("permission").getAsString() : "");
                    stmt.setObject(14, zone.has("min_level") ? zone.get("min_level").getAsInt() : null);
                    stmt.setString(15, zone.has("class_requirement") ? zone.get("class_requirement").getAsString() : null);
                    stmt.setInt(16, zone.get("priority").getAsInt());
                    stmt.setString(17, zone.get("effects_on_spawn").toString());
                    stmt.setInt(18, zone.get("invulnerability_duration").getAsInt());
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + zones.size() + " respawn zones");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate respawn zones", e);
        }
    }
    
    /**
     * Migrate dungeons from JSON.
     */
    private void migrateDungeons(File file) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray dungeons = root.getAsJsonArray("dungeons");
            
            if (dungeons == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO dungeon_definitions 
                (id, name, description, difficulty, min_level, max_level, min_players, max_players,
                 time_limit_minutes, entrance_world, entrance_x, entrance_y, entrance_z, waves_json, rewards_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < dungeons.size(); i++) {
                JsonObject dungeon = dungeons.get(i).getAsJsonObject();
                JsonObject entrance = dungeon.getAsJsonObject("entrance_location");
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, dungeon.get("id").getAsString());
                    stmt.setString(2, dungeon.get("name").getAsString());
                    stmt.setString(3, dungeon.get("description").getAsString());
                    stmt.setString(4, dungeon.get("difficulty").getAsString());
                    stmt.setInt(5, dungeon.get("min_level").getAsInt());
                    stmt.setInt(6, dungeon.get("max_level").getAsInt());
                    stmt.setInt(7, dungeon.get("min_players").getAsInt());
                    stmt.setInt(8, dungeon.get("max_players").getAsInt());
                    stmt.setInt(9, dungeon.get("time_limit_minutes").getAsInt());
                    stmt.setString(10, entrance.get("world").getAsString());
                    stmt.setDouble(11, entrance.get("x").getAsDouble());
                    stmt.setDouble(12, entrance.get("y").getAsDouble());
                    stmt.setDouble(13, entrance.get("z").getAsDouble());
                    stmt.setString(14, dungeon.get("waves").toString());
                    stmt.setString(15, dungeon.get("rewards").toString());
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + dungeons.size() + " dungeons");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate dungeons", e);
        }
    }
    
    /**
     * Migrate invasions/events from JSON.
     */
    private void migrateInvasions(File file) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray events = root.getAsJsonArray("events");
            
            if (events == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO invasions 
                (id, name, description, type, enabled, trigger_type, interval_hours, duration_minutes,
                 waves_json, rewards_json, announcement_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < events.size(); i++) {
                JsonObject event = events.get(i).getAsJsonObject();
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, event.get("id").getAsString());
                    stmt.setString(2, event.get("name").getAsString());
                    stmt.setString(3, event.get("description").getAsString());
                    stmt.setString(4, event.get("type").getAsString());
                    stmt.setInt(5, event.get("enabled").getAsBoolean() ? 1 : 0);
                    stmt.setString(6, event.get("trigger_type").getAsString());
                    stmt.setObject(7, event.has("interval_hours") ? event.get("interval_hours").getAsInt() : null);
                    stmt.setObject(8, event.has("duration_minutes") ? event.get("duration_minutes").getAsInt() : null);
                    stmt.setString(9, event.has("waves") ? event.get("waves").toString() : "[]");
                    stmt.setString(10, event.get("rewards").toString());
                    stmt.setString(11, event.get("announcement").toString());
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + events.size() + " events/invasions");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate invasions", e);
        }
    }
    
    /**
     * Migrate pets from JSON.
     */
    private void migratePets(File file) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray pets = root.getAsJsonArray("pets");
            
            if (pets == null) return;
            
            Connection conn = dbManager.getConnection();
            String sql = """
                INSERT OR REPLACE INTO pets 
                (id, name, description, type, rarity, base_health, base_damage, can_be_mount,
                 mount_speed, min_level_required, adoption_cost, stats_json, abilities_json, food_preference)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            for (int i = 0; i < pets.size(); i++) {
                JsonObject pet = pets.get(i).getAsJsonObject();
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, pet.get("id").getAsString());
                    stmt.setString(2, pet.get("name").getAsString());
                    stmt.setString(3, pet.get("description").getAsString());
                    stmt.setString(4, pet.get("type").getAsString());
                    stmt.setString(5, pet.get("rarity").getAsString());
                    stmt.setDouble(6, pet.get("base_health").getAsDouble());
                    stmt.setDouble(7, pet.get("base_damage").getAsDouble());
                    stmt.setInt(8, pet.get("can_be_mount").getAsBoolean() ? 1 : 0);
                    stmt.setObject(9, pet.has("mount_speed") ? pet.get("mount_speed").getAsDouble() : null);
                    stmt.setInt(10, pet.get("min_level_required").getAsInt());
                    stmt.setInt(11, pet.get("adoption_cost").getAsInt());
                    stmt.setString(12, pet.get("stats_per_level").toString());
                    stmt.setString(13, pet.get("abilities").toString());
                    stmt.setString(14, pet.get("food_preference").toString());
                    stmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Migrated " + pets.size() + " pets");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate pets", e);
        }
    }
    
    /**
     * Generate default data if tables are empty.
     */
    private void generateDefaultData() {
        plugin.getLogger().info("Generating default data...");
        
        // Create default admin user for web panel (password: admin)
        String adminSql = """
            INSERT OR IGNORE INTO admin_users (username, password_hash, created_at, is_active)
            VALUES ('admin', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5jtJ3qKqW6jOe', ?, 1)
            """;
        
        try {
            dbManager.executeUpdate(adminSql, System.currentTimeMillis());
            plugin.getLogger().info("Default admin user created (username: admin, password: admin)");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default admin user", e);
        }
    }
}
