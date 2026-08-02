package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.ILogDAO;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO implements ILogDAO {
    private final DatabaseService databaseService;

    public LogDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    public void addLog(String username, String action) throws SQLException {
        String sql = "INSERT INTO logs (username, action) VALUES (?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<String> getRecentLogs(int limit) throws SQLException {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT TOP " + limit + " * FROM logs ORDER BY id DESC";
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add("[" + rs.getString("timestamp") + "] " + rs.getString("username") + ": " + rs.getString("action"));
            }
        }
        return logs;
    }
}
