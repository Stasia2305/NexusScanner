package com.nexusscan.service;

import com.nexusscan.model.User;
import com.nexusscan.model.Profile;
import com.nexusscan.model.MetadataField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global application state manager.
 * Stores information about the current user, available profiles, and metadata fields.
 * Loads configuration from the database on initialization.
 */
public class AppState {
    private static AppState instance;
    private List<User> users = new ArrayList<>();
    private List<Profile> allProfiles = new ArrayList<>();
    private List<MetadataField> metadataFields = new ArrayList<>();
    private Map<String, List<Profile>> userProfiles = new HashMap<>();
    private User currentUser;

    private AppState() {
        loadFromDatabase();
        if (users.isEmpty()) {
            User admin = new User("admin", "admin", User.Role.ADMIN);
            addUser(admin);
        }
    }

    /**
     * Loads system users, profiles, and metadata field definitions from the SQLite database.
     */
    private void loadFromDatabase() {
        try {
            DatabaseService db = DatabaseService.getInstance();
            // Load Users
            try (Statement stmt = db.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                while (rs.next()) {
                    users.add(new User(rs.getString("username"), rs.getString("password"), User.Role.valueOf(rs.getString("role"))));
                }
            }
            // Load Profiles
            Map<Integer, Profile> profileMap = new HashMap<>();
            try (Statement stmt = db.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM profiles")) {
                while (rs.next()) {
                    String settingsStr = rs.getString("settings");
                    Profile p = new Profile(rs.getString("name"), rs.getString("split_logic"), parseSettings(settingsStr));
                    allProfiles.add(p);
                    profileMap.put(rs.getInt("id"), p);
                }
            }
            // Load Mappings
            try (Statement stmt = db.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM user_profiles")) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    Profile p = profileMap.get(rs.getInt("profile_id"));
                    if (p != null) {
                        userProfiles.computeIfAbsent(username, k -> new ArrayList<>()).add(p);
                    }
                }
            }
            // Load Metadata Fields
            try (Statement stmt = db.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM metadata_fields")) {
                while (rs.next()) {
                    metadataFields.add(new MetadataField(rs.getInt("id"), rs.getString("field_name")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    public void addMetadataField(String name) {
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("INSERT INTO metadata_fields (field_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) metadataFields.add(new MetadataField(rs.getInt(1), name));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMetadataField(String name) {
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("DELETE FROM metadata_fields WHERE field_name = ?")) {
                pstmt.setString(1, name);
                pstmt.executeUpdate();
            }
            metadataFields.removeIf(f -> f.getFieldName().equals(name));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public List<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("INSERT OR REPLACE INTO users (username, password, role) VALUES (?, ?, ?)")) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getPassword());
                pstmt.setString(3, user.getRole().name());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteUser(String username) {
        users.removeIf(u -> u.getUsername().equals(username));
        userProfiles.remove(username);
        try {
            DatabaseService db = DatabaseService.getInstance();
            Connection conn = db.getConnection();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                    pstmt.setString(1, username);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user_profiles WHERE username = ?")) {
                    pstmt.setString(1, username);
                    pstmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Profile> getAllProfiles() {
        return allProfiles;
    }

    public boolean addProfile(Profile profile) {
        if (allProfiles.stream().anyMatch(p -> p.getName().equalsIgnoreCase(profile.getName()))) {
            return false;
        }
        allProfiles.add(profile);
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("INSERT INTO profiles (name, split_logic, settings) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, profile.getName());
                pstmt.setString(2, profile.getSplitLogic());
                pstmt.setString(3, serializeSettings(profile.getSettings()));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String serializeSettings(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
        return sb.toString();
    }

    private Map<String, String> parseSettings(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        String[] pairs = s.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    public void assignProfileToUser(String username, Profile profile) {
        userProfiles.computeIfAbsent(username, k -> new ArrayList<>()).add(profile);
        try {
            DatabaseService db = DatabaseService.getInstance();
            // Get profile ID first
            int profileId = -1;
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("SELECT id FROM profiles WHERE name = ?")) {
                pstmt.setString(1, profile.getName());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) profileId = rs.getInt(1);
                }
            }
            if (profileId != -1) {
                try (PreparedStatement pstmt = db.getConnection().prepareStatement("INSERT INTO user_profiles (username, profile_id) VALUES (?, ?)")) {
                    pstmt.setString(1, username);
                    pstmt.setInt(2, profileId);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Profile> getAccessibleProfiles(String username) {
        return userProfiles.getOrDefault(username, new ArrayList<>());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUsernameSafe() {
        return (currentUser != null) ? currentUser.getUsername() : "SYSTEM";
    }
}
