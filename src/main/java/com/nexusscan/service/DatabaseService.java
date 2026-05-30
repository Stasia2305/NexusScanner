package com.nexusscan.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * The DatabaseService is responsible for managing the connection to the Microsoft SQL Server database.
 * It implements the Singleton pattern to ensure a single connection point.
 */
public class DatabaseService {
    private static DatabaseService instance;
    private final String url;
    private final String user;
    private final String password;

    private DatabaseService() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/db.properties")) {
            if (is == null) {
                throw new RuntimeException("Could not find db.properties");
            }
            props.load(is);
            
            this.url = props.getProperty("db.url");
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
            
            // Initialize database structure
            createTables();
            seedData();
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
        Connection conn;
        if (user != null && password != null) {
            conn = DriverManager.getConnection(url, user, password);
        } else {
            conn = DriverManager.getConnection(url);
        }
        
        // Ensure we are using the correct database
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("USE NexusScan");
        } catch (SQLException e) {
            // If USE fails, we might not have permission or it's already set by connection string
            // We ignore this as the databaseName is already in the URL
        }
        
        return conn;
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Core data tables
            createTableIfNotExists(stmt, "clients", "id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(255)");
            createTableIfNotExists(stmt, "archives", "id INT IDENTITY(1,1) PRIMARY KEY, client_id INT, name VARCHAR(255)");
            createTableIfNotExists(stmt, "boxes", "id INT IDENTITY(1,1) PRIMARY KEY, archive_id INT, box_id_str VARCHAR(255)");
            createTableIfNotExists(stmt, "cases", "id INT IDENTITY(1,1) PRIMARY KEY, box_id INT, case_number VARCHAR(255), metadata NVARCHAR(MAX)");
            createTableIfNotExists(stmt, "documents", "id INT IDENTITY(1,1) PRIMARY KEY, case_id INT, barcode VARCHAR(255), status VARCHAR(50)");
            createTableIfNotExists(stmt, "pages", "id INT IDENTITY(1,1) PRIMARY KEY, document_id INT, page_number INT, image_data VARBINARY(MAX), rotation FLOAT");
            
            // User management tables
            createTableIfNotExists(stmt, "users", "username VARCHAR(255) PRIMARY KEY, password VARCHAR(255), role VARCHAR(50)");
            createTableIfNotExists(stmt, "profiles", "id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(255) UNIQUE, split_logic VARCHAR(255), settings NVARCHAR(MAX)");
            createTableIfNotExists(stmt, "user_profiles", "username VARCHAR(255), profile_id INT");
            
            // Metadata and logging tables
            createTableIfNotExists(stmt, "metadata_fields", "id INT IDENTITY(1,1) PRIMARY KEY, field_name VARCHAR(255) UNIQUE");
            createTableIfNotExists(stmt, "logs", "id INT IDENTITY(1,1) PRIMARY KEY, timestamp DATETIME DEFAULT GETDATE(), username VARCHAR(255), action NVARCHAR(MAX)");
        }
    }

    private void createTableIfNotExists(Statement stmt, String tableName, String columns) throws SQLException {
        String sql = String.format("IF OBJECT_ID('%s', 'U') IS NULL CREATE TABLE %s (%s)", 
                                    tableName, tableName, columns);
        stmt.execute(sql);
    }

    private void seedData() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            seedAdminUser(stmt);
            seedMetadataFields(conn);
            seedDefaultProfile(conn);
            assignDefaultProfileToAdmin(stmt);
        }
    }

    private void seedAdminUser(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users WHERE username = 'admin'");
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin', 'ADMIN')");
        }
    }

    private void seedMetadataFields(Connection conn) throws SQLException {
        String[] defaultFields = {"Case ID", "Client Name", "Document Type"};
        String sql = "IF NOT EXISTS (SELECT 1 FROM metadata_fields WHERE field_name = ?) " +
                     "INSERT INTO metadata_fields (field_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String field : defaultFields) {
                pstmt.setString(1, field);
                pstmt.setString(2, field);
                pstmt.executeUpdate();
            }
        }
    }

    private void seedDefaultProfile(Connection conn) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT 1 FROM profiles WHERE name = ?) " +
                     "INSERT INTO profiles (name, split_logic, settings) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Default Profile");
            pstmt.setString(2, "Default Profile");
            pstmt.setString(3, "5");
            pstmt.setString(4, "rotation=0;quality=high");
            pstmt.executeUpdate();
        }
    }

    private void assignDefaultProfileToAdmin(Statement stmt) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT 1 FROM user_profiles WHERE username = 'admin' AND " +
                     "profile_id = (SELECT id FROM profiles WHERE name = 'Default Profile')) " +
                     "INSERT INTO user_profiles (username, profile_id) " +
                     "SELECT 'admin', id FROM profiles WHERE name = 'Default Profile'";
        stmt.execute(sql);
    }
}
