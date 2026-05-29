package com.nexusscan.service;

import java.sql.*;

/**
 * The DatabaseService is responsible for managing the connection to the database.
 * It creates the necessary tables and makes sure the database structure is up to date.
 */
public class DatabaseService {
    private static DatabaseService instance; // The single copy of DatabaseService used by the whole app
    private Connection connection;

    private DatabaseService() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:nexusscan.db");
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            // Critical: Don't leave connection null if possible or at least log clearly
            throw new RuntimeException("Fatal: Could not initialize database connection.", e);
        }
    }

    /**
     * The getInstance method follows the Singleton pattern.
     * It ensures that only one database connection is created and shared across the entire app.
     */
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * Sets up the database tables if they don't already exist.
     * This creates the structure for organizing Clients, Archives, Boxes, and other data.
     */
    private void createTables() throws SQLException {
        if (connection == null) throw new SQLException("Connection to the database failed.");
        try (Statement stmt = connection.createStatement()) {
            // Create core data tables
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS archives (id INTEGER PRIMARY KEY AUTOINCREMENT, client_id INTEGER, name TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS boxes (id INTEGER PRIMARY KEY AUTOINCREMENT, archive_id INTEGER, box_id_str TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cases (id INTEGER PRIMARY KEY AUTOINCREMENT, box_id INTEGER, case_number TEXT, metadata TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS documents (id INTEGER PRIMARY KEY AUTOINCREMENT, case_id INTEGER, barcode TEXT, status TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS pages (id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER, page_number INTEGER, image_data BLOB, rotation REAL)");
            
            // Create user management and configuration tables
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, role TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, split_logic TEXT, settings TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS user_profiles (username TEXT, profile_id INTEGER)");
            
            // Create metadata and logging tables
            stmt.execute("CREATE TABLE IF NOT EXISTS metadata_fields (id INTEGER PRIMARY KEY AUTOINCREMENT, field_name TEXT UNIQUE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, username TEXT, action TEXT)");
        }
        
        // Ensure the database is up to date by checking for missing columns
        ensureColumnExists("profiles", "settings", "TEXT");
    }

    /**
     * A helper method to add a new column to an existing table if it's missing.
     * This is useful for updating the database structure without losing data.
     */
    private void ensureColumnExists(String tableName, String columnName, String columnType) {
        try (Statement stmt = connection.createStatement()) {
            // SQLite doesn't have IF NOT EXISTS for ADD COLUMN
            // Check if column exists by querying table info
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
            boolean exists = false;
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:nexusscan.db");
        }
        return connection;
    }
}
