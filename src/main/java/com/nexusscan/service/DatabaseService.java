package com.nexusscan.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * The DatabaseService is responsible for managing the connection to the MSSQL database.
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
            
            this.url = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=%s;trustServerCertificate=%s;",
                    props.getProperty("db.server"),
                    props.getProperty("db.port"),
                    props.getProperty("db.database"),
                    props.getProperty("db.encrypt"),
                    props.getProperty("db.trustServerCertificate"));
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
            
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
        return DriverManager.getConnection(url, user, password);
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create core data tables if they don't exist (MSSQL syntax)
            executeIfNotExists(stmt, "clients", "CREATE TABLE clients (id INT IDENTITY(1,1) PRIMARY KEY, name NVARCHAR(255))");
            executeIfNotExists(stmt, "archives", "CREATE TABLE archives (id INT IDENTITY(1,1) PRIMARY KEY, client_id INT, name NVARCHAR(255))");
            executeIfNotExists(stmt, "boxes", "CREATE TABLE boxes (id INT IDENTITY(1,1) PRIMARY KEY, archive_id INT, box_id_str NVARCHAR(255))");
            executeIfNotExists(stmt, "cases", "CREATE TABLE cases (id INT IDENTITY(1,1) PRIMARY KEY, box_id INT, case_number NVARCHAR(255), metadata NVARCHAR(MAX))");
            executeIfNotExists(stmt, "documents", "CREATE TABLE documents (id INT IDENTITY(1,1) PRIMARY KEY, case_id INT, barcode NVARCHAR(255), status NVARCHAR(50))");
            executeIfNotExists(stmt, "pages", "CREATE TABLE pages (id INT IDENTITY(1,1) PRIMARY KEY, document_id INT, page_number INT, image_data VARBINARY(MAX), rotation FLOAT)");
            
            // Create user management and configuration tables
            executeIfNotExists(stmt, "users", "CREATE TABLE users (username NVARCHAR(255) PRIMARY KEY, password NVARCHAR(255), role NVARCHAR(50))");
            executeIfNotExists(stmt, "profiles", "CREATE TABLE profiles (id INT IDENTITY(1,1) PRIMARY KEY, name NVARCHAR(255) UNIQUE, split_logic NVARCHAR(255), settings NVARCHAR(MAX))");
            executeIfNotExists(stmt, "user_profiles", "CREATE TABLE user_profiles (username NVARCHAR(255), profile_id INT)");
            
            // Create metadata and logging tables
            executeIfNotExists(stmt, "metadata_fields", "CREATE TABLE metadata_fields (id INT IDENTITY(1,1) PRIMARY KEY, field_name NVARCHAR(255) UNIQUE)");
            executeIfNotExists(stmt, "logs", "CREATE TABLE logs (id INT IDENTITY(1,1) PRIMARY KEY, timestamp DATETIME DEFAULT GETDATE(), username NVARCHAR(255), action NVARCHAR(MAX))");
        }
    }

    private void executeIfNotExists(Statement stmt, String tableName, String createSql) throws SQLException {
        String checkSql = String.format("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '%s') BEGIN %s END", tableName, createSql);
        stmt.execute(checkSql);
    }
}
