package com.nexusscan.dal.interfaces;

import java.sql.SQLException;
import java.util.List;

public interface ILogDAO {
    void addLog(String username, String action) throws SQLException;
    List<String> getRecentLogs(int limit) throws SQLException;
}
