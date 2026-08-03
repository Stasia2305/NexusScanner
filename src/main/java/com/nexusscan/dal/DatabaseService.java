package com.nexusscan.dal;

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
    private boolean connectionFailed = false;

    /**
     * Private constructor for the Singleton pattern.
     * Initializes connection properties and sets a fail-fast timeout.
     */
    private DatabaseService() {
        Properties props = new Properties();
        String tempUrl = null;
        String tempUser = null;
        String tempPass = null;
        
        try (InputStream is = getClass().getResourceAsStream("/db.properties")) {
            if (is != null) {
                props.load(is);
                tempUrl = props.getProperty("db.url");
                tempUser = props.getProperty("db.user");
                tempPass = props.getProperty("db.password");
                
                // Set a short login timeout (5s) to prevent the application from hanging
                // if the school server (10.176.111.34) is unreachable.
                DriverManager.setLoginTimeout(5);
            } else {
                System.err.println("WARNING: Could not find db.properties.");
            }
        } catch (IOException e) {
            System.err.println("WARNING: Could not load db.properties: " + e.getMessage());
        }

        this.url = tempUrl;
        this.user = tempUser;
        this.password = tempPass;

        if (this.url != null) {
            try {
                // Ensure the NexusScan database exists on the server
                ensureDatabaseExists();
                // Initialize the table structure for Clients, Archives, Boxes, etc.
                createTables();
                seedData();
            } catch (SQLException e) {
                System.err.println("WARNING: Could not initialize database connection: " + e.getMessage());
                // Mark connection as failed to enable offline fallback mode
                connectionFailed = true;
            }
        } else {
            System.err.println("WARNING: Database configuration missing. Database features will be unavailable.");
            connectionFailed = true;
        }
    }

    /**
     * Verifies if the target database exists; attempts to create it if it doesn't.
     */
    private void ensureDatabaseExists() throws SQLException {
        // Connect to the base server to check for/create the NexusScan database
        String baseUrl = url.replaceAll(";databaseName=[^;]*", "");
        try (Connection conn = (user != null && password != null) ? 
                DriverManager.getConnection(baseUrl, user, password) : 
                DriverManager.getConnection(baseUrl)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'NexusScan') CREATE DATABASE NexusScan");
            }
        } catch (SQLException e) {
            // Failure here is often due to permissions; we continue and let getConnection() handle specifics
        }
    }

    /**
     * Provides the global instance of the DatabaseService.
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * Attempts to establish a connection to the MSSQL database.
     * If the database is unreachable, it throws an exception to trigger offline fallbacks.
     */
    public Connection getConnection() throws SQLException {
        if (url == null || connectionFailed) {
            throw new SQLException("Database connection not available (Offline Mode).");
        }
        Connection conn;
        try {
            if (user != null && password != null) {
                conn = DriverManager.getConnection(url, user, password);
            } else {
                conn = DriverManager.getConnection(url);
            }
            
            // Explicitly ensure we are working within the NexusScan context
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("USE NexusScan");
            } catch (SQLException e) {
                // Ignore if USE fails; databaseName is typically handled in the JDBC URL
            }
            
            return conn;
        } catch (SQLException e) {
            // Permanently mark connection as failed for this session to avoid repeated timeouts
            connectionFailed = true;
            throw e;
        }
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
            createTableIfNotExists(stmt, "users", "username VARCHAR(255) PRIMARY KEY, password VARCHAR(255), email VARCHAR(255), role VARCHAR(50)");
            createTableIfNotExists(stmt, "profiles", "id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(255) UNIQUE, split_logic VARCHAR(255), settings NVARCHAR(MAX), description NVARCHAR(MAX)");
            createTableIfNotExists(stmt, "user_profiles", "username VARCHAR(255), profile_id INT");
            
            // Migration logic for existing tables
            ensureColumnExists(stmt, "users", "email", "VARCHAR(255)");
            ensureColumnExists(stmt, "profiles", "description", "NVARCHAR(MAX)");
            
            // Metadata and logging tables
            createTableIfNotExists(stmt, "metadata_fields", "id INT IDENTITY(1,1) PRIMARY KEY, field_name VARCHAR(255) UNIQUE");
            createTableIfNotExists(stmt, "logs", "id INT IDENTITY(1,1) PRIMARY KEY, timestamp DATETIME DEFAULT GETDATE(), username VARCHAR(255), action NVARCHAR(MAX)");
        }
    }

    private void ensureColumnExists(Statement stmt, String tableName, String columnName, String type) throws SQLException {
        String sql = String.format("IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('%s') AND name = '%s') " +
                                    "ALTER TABLE %s ADD %s %s", tableName, columnName, tableName, columnName, type);
        stmt.execute(sql);
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
        try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users WHERE username = 'admin'")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, email, role) VALUES ('admin', 'admin', 'admin@nexusscan.com', 'ADMIN')");
            }
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
                     "INSERT INTO profiles (name, split_logic, settings, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Default Profile");
            pstmt.setString(2, "Default Profile");
            pstmt.setString(3, "5");
            pstmt.setString(4, "rotation=0;quality=high");
            pstmt.setString(5, "Standard scanning configuration");
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
