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
                
                // Set login timeout (5s) to avoid long hangs if server is unreachable
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
                // Initialize database and tables
                ensureDatabaseExists();
                createTables();
                seedData();
            } catch (SQLException e) {
                System.err.println("WARNING: Could not initialize database: " + e.getMessage());
                connectionFailed = true; // Switch to offline mode
            }
        } else {
            System.err.println("WARNING: Database configuration missing. Using offline mode.");
            connectionFailed = true;
        }
    }

    /**
     * Attempts to create the NexusScan database if it does not already exist.
     */
    private void ensureDatabaseExists() throws SQLException {
        // Strip databaseName from URL to connect to the server root
        String baseUrl = url.replaceAll(";databaseName=[^;]*", "");
        try (Connection conn = (user != null && password != null) ? 
                DriverManager.getConnection(baseUrl, user, password) : 
                DriverManager.getConnection(baseUrl)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'NexusScan') CREATE DATABASE NexusScan");
            }
        } catch (SQLException e) {
            // Permissions might prevent database creation; we attempt to proceed regardless
        }
    }

    /**
     * Provides the global instance of the DatabaseService.
     *
     * @return The Singleton instance of DatabaseService.
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * Establishes and returns a new connection to the MSSQL database.
     * 
     * @return A SQL Connection object.
     * @throws SQLException If the connection fails or if in offline mode.
     */
    public Connection getConnection() throws SQLException {
        if (url == null || connectionFailed) {
            throw new SQLException("Database connection not available (Offline Mode).");
        }
        
        try {
            Connection conn = (user != null && password != null) ? 
                    DriverManager.getConnection(url, user, password) : 
                    DriverManager.getConnection(url);
            
            // Ensure we use the correct database context
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("USE NexusScan");
            } catch (SQLException e) {
                // Ignore if USE fails; database name is usually in the URL
            }
            
            return conn;
        } catch (SQLException e) {
            connectionFailed = true; // Avoid repeated timeouts
            throw e;
        }
    }

    /**
     * Creates required tables if they do not already exist in the database.
     * Also performs any necessary schema migrations.
     *
     * @throws SQLException If table creation fails.
     */
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

    /**
     * Helper method to ensure a column exists in a table, adding it if missing.
     *
     * @param stmt       The database Statement to execute.
     * @param tableName  The name of the target table.
     * @param columnName The name of the column to verify/add.
     * @param type       The SQL type of the column.
     * @throws SQLException If SQL execution fails.
     */
    private void ensureColumnExists(Statement stmt, String tableName, String columnName, String type) throws SQLException {
        String sql = String.format("IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('%s') AND name = '%s') " +
                                    "ALTER TABLE %s ADD %s %s", tableName, columnName, tableName, columnName, type);
        stmt.execute(sql);
    }

    /**
     * Helper method to create a table if it does not already exist.
     *
     * @param stmt      The database Statement to execute.
     * @param tableName The name of the table to create.
     * @param columns   The column definitions SQL string.
     * @throws SQLException If table creation fails.
     */
    private void createTableIfNotExists(Statement stmt, String tableName, String columns) throws SQLException {
        String sql = String.format("IF OBJECT_ID('%s', 'U') IS NULL CREATE TABLE %s (%s)", 
                                    tableName, tableName, columns);
        stmt.execute(sql);
    }

    /**
     * Seeds initial static and default data into the database.
     *
     * @throws SQLException If seeding fails.
     */
    private void seedData() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            seedAdminUser(stmt);
            seedMetadataFields(conn);
            seedDefaultProfile(conn);
            assignDefaultProfileToAdmin(stmt);
        }
    }

    /**
     * Seeds the default admin user if no users exist with the 'admin' username.
     *
     * @param stmt The database Statement to execute.
     * @throws SQLException If database execution fails.
     */
    private void seedAdminUser(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users WHERE username = 'admin'")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, email, role) VALUES ('admin', 'admin', 'admin@nexusscan.com', 'ADMIN')");
            }
        }
    }

    /**
     * Seeds default metadata fields if they do not exist.
     *
     * @param conn The database Connection to use.
     * @throws SQLException If database execution fails.
     */
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

    /**
     * Seeds the default scanning profile if it does not exist.
     *
     * @param conn The database Connection to use.
     * @throws SQLException If database execution fails.
     */
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

    /**
     * Assigns the default scanning profile to the 'admin' user if not already assigned.
     *
     * @param stmt The database Statement to execute.
     * @throws SQLException If database execution fails.
     */
    private void assignDefaultProfileToAdmin(Statement stmt) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT 1 FROM user_profiles WHERE username = 'admin' AND " +
                     "profile_id = (SELECT id FROM profiles WHERE name = 'Default Profile')) " +
                     "INSERT INTO user_profiles (username, profile_id) " +
                     "SELECT 'admin', id FROM profiles WHERE name = 'Default Profile'";
        stmt.execute(sql);
    }
}
