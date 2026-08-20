package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.ILogDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The LoggingService records important actions taken in the application.
 */
public class LoggingService {
    private static LoggingService instance;
    private final ILogDAO logDAO;

    /**
     * Private constructor for singleton pattern.
     */
    private LoggingService() {
        this.logDAO = DAOFactory.getLogDAO();
    }

    /**
     * Gets the Singleton instance of LoggingService.
     *
     * @return The LoggingService instance.
     */
    public static synchronized LoggingService getInstance() {
        if (instance == null) {
            instance = new LoggingService();
        }
        return instance;
    }

    /**
     * Logs an action associated with a specific username.
     *
     * @param action   A description of the action.
     * @param username The username of the user performing the action.
     */
    public void log(String action, String username) {
        try {
            logDAO.addLog(username, action);
        } catch (SQLException e) {
            // Log error locally if DB fails
        }
    }

    /**
     * Retrieves the most recent system logs up to 100 entries.
     *
     * @return A list of log descriptions.
     */
    public List<String> getRecentLogs() {
        try {
            return logDAO.getRecentLogs(100);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }
}
