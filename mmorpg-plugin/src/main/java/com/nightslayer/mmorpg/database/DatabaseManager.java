package com.nightslayer.mmorpg.database;

import com.nightslayer.mmorpg.MMORPGPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Singleton Database Manager for SQLite connections.
 * 
 * CRITICAL WARNINGS:
 * - This class maintains a SINGLE connection (singleton pattern)
 * - NEVER close the Connection in try-with-resources
 * - ONLY use try-with-resources for Statement/PreparedStatement/ResultSet
 * - Connection is closed only in closeConnection() method
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private final MMORPGPlugin plugin;
    private Connection connection;
    private final String databasePath;
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param plugin The plugin instance
     * @param databasePath Path to the SQLite database file
     */
    private DatabaseManager(MMORPGPlugin plugin, String databasePath) {
        this.plugin = plugin;
        this.databasePath = databasePath;
    }
    
    /**
     * Get the singleton instance of DatabaseManager.
     * 
     * @param plugin The plugin instance
     * @param databasePath Path to the database
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance(MMORPGPlugin plugin, String databasePath) {
        if (instance == null) {
            instance = new DatabaseManager(plugin, databasePath);
        }
        return instance;
    }
    
    /**
     * Get the current instance (must be initialized first).
     * 
     * @return DatabaseManager instance
     * @throws IllegalStateException if not initialized
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager not initialized!");
        }
        return instance;
    }
    
    /**
     * Initialize the database connection.
     * 
     * @return true if successful, false otherwise
     */
    public boolean initializeConnection() {
        try {
            // Create database file and parent directories if they don't exist
            File dbFile = new File(databasePath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection with optimized settings
            String url = "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);
            
            // Enable foreign keys and set pragmas for better performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA temp_store = MEMORY");
                stmt.execute("PRAGMA cache_size = 10000");
            }
            
            plugin.getLogger().info("Database connection established: " + databasePath);
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found!");
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get the database connection.
     * WARNING: NEVER close this connection! It's a singleton.
     * 
     * @return Connection instance
     * @throws SQLException if connection is closed or null
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().warning("Database connection was closed, reinitializing...");
            initializeConnection();
        }
        return connection;
    }
    
    /**
     * Execute an UPDATE, INSERT, or DELETE statement.
     * 
     * @param sql SQL statement
     * @param params Parameters for prepared statement
     * @return Number of affected rows
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            setParameters(stmt, params);
            int result = stmt.executeUpdate();
            return result;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing update: " + sql, e);
            return -1;
        }
    }
    
    /**
     * Execute a SELECT query.
     * 
     * @param sql SQL query
     * @param params Parameters for prepared statement
     * @return ResultSet (caller must close it!)
     */
    public ResultSet executeQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = getConnection().prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing query: " + sql, e);
            return null;
        }
    }
    
    /**
     * Execute a query and process results with a callback.
     * Automatically closes resources.
     * 
     * @param sql SQL query
     * @param callback Callback to process results
     * @param params Parameters for prepared statement
     */
    public void executeQueryWithCallback(String sql, ResultSetCallback callback, Object... params) {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                callback.process(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing query with callback: " + sql, e);
        }
    }
    
    /**
     * Execute an async UPDATE, INSERT, or DELETE statement.
     * 
     * @param sql SQL statement
     * @param params Parameters for prepared statement
     * @return CompletableFuture with number of affected rows
     */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params));
    }
    
    /**
     * Execute an async SELECT query.
     * 
     * @param sql SQL query
     * @param callback Callback to process results
     * @param params Parameters for prepared statement
     * @return CompletableFuture
     */
    public CompletableFuture<Void> executeQueryAsync(String sql, ResultSetCallback callback, Object... params) {
        return CompletableFuture.runAsync(() -> executeQueryWithCallback(sql, callback, params));
    }
    
    /**
     * Execute a batch of SQL statements.
     * 
     * @param sqlStatements List of SQL statements with their parameters
     * @return true if all successful, false otherwise
     */
    public boolean executeBatch(List<BatchStatement> sqlStatements) {
        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get connection for batch execution!");
            e.printStackTrace();
            return false;
        }
        
        boolean originalAutoCommit;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to disable auto-commit!");
            e.printStackTrace();
            return false;
        }
        
        try {
            for (BatchStatement batchStmt : sqlStatements) {
                try (PreparedStatement stmt = conn.prepareStatement(batchStmt.sql)) {
                    setParameters(stmt, batchStmt.params);
                    stmt.execute();
                }
            }
            
            conn.commit();
            conn.setAutoCommit(originalAutoCommit);
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error executing batch, rolling back!");
            e.printStackTrace();
            try {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException rollbackEx) {
                plugin.getLogger().severe("Failed to rollback!");
                rollbackEx.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Check if a table exists in the database.
     * 
     * @param tableName Table name
     * @return true if exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking table existence: " + tableName, e);
            return false;
        }
    }
    
    /**
     * Set parameters on a prepared statement.
     * 
     * @param stmt PreparedStatement
     * @param params Parameters to set
     * @throws SQLException if error occurs
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Close the database connection.
     * Should only be called when plugin is disabled.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error closing database connection!", e);
            } finally {
                connection = null;
                instance = null;
            }
        }
    }
    
    /**
     * Callback interface for processing ResultSets.
     */
    @FunctionalInterface
    public interface ResultSetCallback {
        void process(ResultSet rs) throws SQLException;
    }
    
    /**
     * Check if database is connected.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Helper class for batch statements.
     */
    public static class BatchStatement {
        public final String sql;
        public final Object[] params;
        
        public BatchStatement(String sql, Object... params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
