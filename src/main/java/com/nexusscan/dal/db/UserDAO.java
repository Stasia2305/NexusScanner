package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.IUserDAO;
import com.nexusscan.model.User;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements IUserDAO {
    private final DatabaseService databaseService;

    public UserDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    private User.Role parseRole(String roleStr) {
        if (roleStr == null) return User.Role.USER;
        try {
            return User.Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return User.Role.USER;
        }
    }

    /**
     * Retrieves a user by their username.
     * Includes fallback logic for 'admin' and 'user' accounts if the database is offline.
     */
    @Override
    public User getUser(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            parseRole(rs.getString("role"))
                    );
                }
            }
        } catch (SQLException e) {
            // Fallback for offline/demo mode: allow access with default credentials
            if ("admin".equals(username)) {
                return new User("admin", "admin", "admin@nexusscan.com", User.Role.ADMIN);
            }
            if ("user".equals(username)) {
                return new User("user", "user", "user@nexusscan.com", User.Role.USER);
            }
            throw e;
        }
        return null;
    }

    /**
     * Retrieves a list of all users from the database.
     * Falls back to returning the default 'admin' and 'user' accounts in offline mode.
     */
    @Override
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        parseRole(rs.getString("role"))
                ));
            }
        } catch (SQLException e) {
            // Fallback for offline mode: provide the default system accounts
            users.add(new User("admin", "admin", "admin@nexusscan.com", User.Role.ADMIN));
            users.add(new User("user", "user", "user@nexusscan.com", User.Role.USER));
        }
        return users;
    }

    @Override
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole().name());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteUser(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }
}
