package com.nightslayer.mmorpg.database;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manages world-specific databases.
 * Each world has its own SQLite database for local data (player_stats, kills, etc.)
 * 
 * CRITICAL WARNING:
 * - DO NOT use getCanonicalFile() - it breaks symlinks!
 * - Use resolve() to handle symlinks properly
 * - Each world database is separate from the universal database
 */
public class WorldDatabaseManager {
    
    private final MMORPGPlugin plugin;
    private Connection worldConnection;
    private String currentWorldPath;
    
    public WorldDatabaseManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the world database for the active world.
     * Resolves the 'active' symlink to get the actual world.
     * 
     * @return true if successful
     */
    public boolean initializeWorldDatabase() {
        try {
            // Get the active world path through symlink
            File serverDir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "minecraft-server");
            File worldsDir = new File(serverDir, "worlds");
            File activeLink = new File(worldsDir, "active");
            
            if (!activeLink.exists()) {
                plugin.getLogger().warning("Active world symlink not found: " + activeLink.getPath());
                // Fallback to default world
                activeLink = new File(worldsDir, "mundo-inicial");
            }
            
            // Resolve symlink WITHOUT using getCanonicalFile() to preserve symlink info
            Path activePath = activeLink.toPath();
            Path realPath;
            
            if (Files.isSymbolicLink(activePath)) {
                realPath = Files.readSymbolicLink(activePath);
                // If relative, resolve against parent
                if (!realPath.isAbsolute()) {
                    realPath = activePath.getParent().resolve(realPath).normalize();
                }
            } else {
                realPath = activePath;
            }
            
            File worldDir = realPath.toFile();
            File worldDataDir = new File(new File(worldDir, "world"), "data");
            
            if (!worldDataDir.exists()) {
                worldDataDir.mkdirs();
            }
            
            File worldDb = new File(worldDataDir, "world.db");
            currentWorldPath = worldDb.getAbsolutePath();
            
            // Initialize connection
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + currentWorldPath;
            worldConnection = DriverManager.getConnection(url);
            
            // Enable foreign keys and optimizations
            try (PreparedStatement stmt = worldConnection.prepareStatement("PRAGMA foreign_keys = ON")) {
                stmt.execute();
            }
            try (PreparedStatement stmt = worldConnection.prepareStatement("PRAGMA journal_mode = WAL")) {
                stmt.execute();
            }
            
            // Create world-specific tables
            createWorldTables();
            
            plugin.getLogger().info("World database initialized: " + currentWorldPath);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize world database!", e);
            return false;
        }
    }
    
    /**
     * Create tables specific to this world.
     */
    private void createWorldTables() throws SQLException {
        // Player stats per world
        String playerStatsTable = """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid TEXT PRIMARY KEY,
                kills INTEGER DEFAULT 0,
                deaths INTEGER DEFAULT 0,
                playtime_seconds INTEGER DEFAULT 0,
                blocks_broken INTEGER DEFAULT 0,
                blocks_placed INTEGER DEFAULT 0,
                distance_walked REAL DEFAULT 0.0,
                jumps INTEGER DEFAULT 0,
                damage_dealt REAL DEFAULT 0.0,
                damage_taken REAL DEFAULT 0.0,
                first_join INTEGER,
                last_seen INTEGER
            )
            """;
        
        // Kills tracking (what mobs player killed)
        String killsTable = """
            CREATE TABLE IF NOT EXISTS kills_tracking (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                mob_type TEXT NOT NULL,
                mob_level INTEGER,
                timestamp INTEGER,
                location_x REAL,
                location_y REAL,
                location_z REAL
            )
            """;
        
        // Deaths tracking
        String deathsTable = """
            CREATE TABLE IF NOT EXISTS deaths_tracking (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                cause TEXT,
                killer_type TEXT,
                timestamp INTEGER,
                location_x REAL,
                location_y REAL,
                location_z REAL,
                items_lost TEXT
            )
            """;
        
        // World events log
        String eventsTable = """
            CREATE TABLE IF NOT EXISTS world_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_type TEXT NOT NULL,
                description TEXT,
                participants TEXT,
                timestamp INTEGER,
                location_x REAL,
                location_y REAL,
                location_z REAL
            )
            """;
        
        try (PreparedStatement stmt = worldConnection.prepareStatement(playerStatsTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = worldConnection.prepareStatement(killsTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = worldConnection.prepareStatement(deathsTable)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = worldConnection.prepareStatement(eventsTable)) {
            stmt.execute();
        }
    }
    
    /**
     * Get the world database connection.
     * 
     * @return Connection to world database
     * @throws SQLException if connection is closed
     */
    public Connection getWorldConnection() throws SQLException {
        if (worldConnection == null || worldConnection.isClosed()) {
            plugin.getLogger().warning("World database connection was closed, reinitializing...");
            initializeWorldDatabase();
        }
        return worldConnection;
    }
    
    /**
     * Execute an update on world database.
     * 
     * @param sql SQL statement
     * @param params Parameters
     * @return Number of affected rows
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = getWorldConnection().prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing world DB update: " + sql, e);
            return -1;
        }
    }
    
    /**
     * Execute a query on world database with callback.
     * 
     * @param sql SQL query
     * @param callback Callback to process results
     * @param params Parameters
     */
    public void executeQuery(String sql, DatabaseManager.ResultSetCallback callback, Object... params) {
        try (PreparedStatement stmt = getWorldConnection().prepareStatement(sql)) {
            setParameters(stmt, params);
            callback.process(stmt.executeQuery());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing world DB query: " + sql, e);
        }
    }
    
    /**
     * Record a mob kill in world database.
     * 
     * @param playerUuid Player UUID
     * @param mobType Mob type
     * @param mobLevel Mob level
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void recordKill(String playerUuid, String mobType, int mobLevel, double x, double y, double z) {
        String sql = """
            INSERT INTO kills_tracking (player_uuid, mob_type, mob_level, timestamp, location_x, location_y, location_z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        executeUpdate(sql, playerUuid, mobType, mobLevel, System.currentTimeMillis(), x, y, z);
        
        // Update player stats
        String updateStats = """
            INSERT INTO player_stats (player_uuid, kills) VALUES (?, 1)
            ON CONFLICT(player_uuid) DO UPDATE SET kills = kills + 1
            """;
        
        executeUpdate(updateStats, playerUuid);
    }
    
    /**
     * Record a player death in world database.
     * 
     * @param playerUuid Player UUID
     * @param cause Death cause
     * @param killerType Killer type (if any)
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param itemsLost JSON of items lost
     */
    public void recordDeath(String playerUuid, String cause, String killerType, double x, double y, double z, String itemsLost) {
        String sql = """
            INSERT INTO deaths_tracking (player_uuid, cause, killer_type, timestamp, location_x, location_y, location_z, items_lost)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        executeUpdate(sql, playerUuid, cause, killerType, System.currentTimeMillis(), x, y, z, itemsLost);
        
        // Update player stats
        String updateStats = """
            INSERT INTO player_stats (player_uuid, deaths) VALUES (?, 1)
            ON CONFLICT(player_uuid) DO UPDATE SET deaths = deaths + 1
            """;
        
        executeUpdate(updateStats, playerUuid);
    }
    
    /**
     * Update player playtime.
     * 
     * @param playerUuid Player UUID
     * @param additionalSeconds Seconds to add
     */
    public void updatePlaytime(String playerUuid, long additionalSeconds) {
        String sql = """
            INSERT INTO player_stats (player_uuid, playtime_seconds, last_seen) VALUES (?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET 
                playtime_seconds = playtime_seconds + ?,
                last_seen = ?
            """;
        
        long now = System.currentTimeMillis();
        executeUpdate(sql, playerUuid, additionalSeconds, now, additionalSeconds, now);
    }
    
    /**
     * Set parameters on prepared statement.
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Close the world database connection.
     */
    public void closeConnection() {
        if (worldConnection != null) {
            try {
                if (!worldConnection.isClosed()) {
                    worldConnection.close();
                    plugin.getLogger().info("World database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error closing world database connection!", e);
            } finally {
                worldConnection = null;
            }
        }
    }
    
    /**
     * Close all world database connections.
     */
    public void closeAllConnections() {
        try {
            if (worldConnection != null && !worldConnection.isClosed()) {
                worldConnection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing world database connection: " + e.getMessage());
        } finally {
            worldConnection = null;
        }
    }
    
    /**
     * Get the current world database path.
     * 
     * @return Path to world database
     */
    public String getCurrentWorldPath() {
        return currentWorldPath;
    }
}
