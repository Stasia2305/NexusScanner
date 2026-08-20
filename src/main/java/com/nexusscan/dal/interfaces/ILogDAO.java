package com.nexusscan.dal.interfaces;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Data Access Objects managing audit/action logs.
 */
public interface ILogDAO {
    /**
     * Adds an action log entry to the database.
     *
     * @param username The username of the user who performed the action.
     * @param action   A description of the action performed.
     * @throws SQLException If a database error occurs.
     */
    void addLog(String username, String action) throws SQLException;

    /**
     * Retrieves the most recent log entries up to a specified limit.
     *
     * @param limit The maximum number of log entries to retrieve.
     * @return A list of log descriptions.
     * @throws SQLException If a database error occurs.
     */
    List<String> getRecentLogs(int limit) throws SQLException;
}
