package com.nexusscan.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton service for system auditing and logging.
 * Persists user actions and system events to the SQLite database for traceability.
 */
public class LoggingService {
    private static LoggingService instance;
    private List<LogEntry> logs = new ArrayList<>();

    /**
     * Data structure for a single audit log entry.
     */
    public static class LogEntry {
        private String action;
        private String user;
        private LocalDateTime timestamp;

        public LogEntry(String action, String user) {
            this.action = action;
            this.user = user;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return "[" + timestamp + "] " + user + ": " + action;
        }
    }

    private LoggingService() {}

    public static LoggingService getInstance() {
        if (instance == null) {
            instance = new LoggingService();
        }
        return instance;
    }

    /**
     * Records a system action performed by a user.
     * Logs to standard output, internal list, and the persistent database.
     */
    public void log(String action, String user) {
        LogEntry entry = new LogEntry(action, user);
        logs.add(entry);
        System.out.println(entry); 
        
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("INSERT INTO logs (timestamp, username, action) VALUES (?, ?, ?)")) {
                pstmt.setString(1, entry.timestamp.toString());
                pstmt.setString(2, user);
                pstmt.setString(3, action);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<LogEntry> getLogs() {
        return logs;
    }
}
