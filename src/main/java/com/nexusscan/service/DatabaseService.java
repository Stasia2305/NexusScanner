package com.nexusscan.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * The DatabaseService is responsible for managing the connection to the SQLite database.
 * It implements the Singleton pattern to ensure a single connection point.
 */
public class DatabaseService {
    private static DatabaseService instance;
    private final String url;

    private DatabaseService() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/db.properties")) {
            if (is == null) {
                throw new RuntimeException("Could not find db.properties");
            }
            props.load(is);
            
            this.url = props.getProperty("db.url", "jdbc:sqlite:nexusscan.db");
            
            // Initialize database structure
            createTables();
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Fatal: Could not initialize database connection.", e);
        }
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create core data tables if they don't exist (SQLite syntax)
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS archives (id INTEGER PRIMARY KEY AUTOINCREMENT, client_id INTEGER, name VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS boxes (id INTEGER PRIMARY KEY AUTOINCREMENT, archive_id INTEGER, box_id_str VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS cases (id INTEGER PRIMARY KEY AUTOINCREMENT, box_id INTEGER, case_number VARCHAR(255), metadata TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS documents (id INTEGER PRIMARY KEY AUTOINCREMENT, case_id INTEGER, barcode VARCHAR(255), status VARCHAR(50))");
            stmt.execute("CREATE TABLE IF NOT EXISTS pages (id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER, page_number INTEGER, image_data BLOB, rotation FLOAT)");
            
            // Create user management and configuration tables
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username VARCHAR(255) PRIMARY KEY, password VARCHAR(255), role VARCHAR(50))");
            stmt.execute("CREATE TABLE IF NOT EXISTS profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(255) UNIQUE, split_logic VARCHAR(255), settings TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS user_profiles (username VARCHAR(255), profile_id INTEGER)");
            
            // Create metadata and logging tables
            stmt.execute("CREATE TABLE IF NOT EXISTS metadata_fields (id INTEGER PRIMARY KEY AUTOINCREMENT, field_name VARCHAR(255) UNIQUE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, username VARCHAR(255), action TEXT)");
        }
    }
}
