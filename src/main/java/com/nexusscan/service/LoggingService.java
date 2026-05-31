package com.nexusscan.service;

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

    private LoggingService() {
        this.logDAO = DAOFactory.getLogDAO();
    }

    public static synchronized LoggingService getInstance() {
        if (instance == null) {
            instance = new LoggingService();
        }
        return instance;
    }

    public void log(String action, String username) {
        try {
            logDAO.addLog(username, action);
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    public List<String> getRecentLogs() {
        try {
            return logDAO.getRecentLogs(100);
        } catch (SQLException e) {
            //e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
