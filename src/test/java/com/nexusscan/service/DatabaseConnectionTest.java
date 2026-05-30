package com.nexusscan.service;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the DatabaseService correctly connects to the Microsoft SQL Server database
 * and initializes the required tables.
 */
public class DatabaseConnectionTest {

    @Test
    void testConnection() throws SQLException {
        DatabaseService dbService = DatabaseService.getInstance();
        try (Connection conn = dbService.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        }
    }

    @Test
    void testTablesExist() throws SQLException {
        DatabaseService dbService = DatabaseService.getInstance();
        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String[] tables = {
                "clients", "archives", "boxes", "cases", "documents", 
                "pages", "users", "profiles", "user_profiles", 
                "metadata_fields", "logs"
            };

            for (String tableName : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM sys.tables WHERE name='" + tableName + "'")) {
                    assertTrue(rs.next(), "Table '" + tableName + "' should exist");
                }
            }
        }
    }
}
