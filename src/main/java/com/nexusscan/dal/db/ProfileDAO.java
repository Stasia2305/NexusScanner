package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.IProfileDAO;
import com.nexusscan.model.Profile;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL Server implementation for managing scanning profiles and user associations.
 */
public class ProfileDAO implements IProfileDAO {
    private final DatabaseService databaseService;

    /**
     * Constructs a new ProfileDAO and obtains the database service instance.
     */
    public ProfileDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    /**
     * Retrieves all scanning profiles from the database.
     * Falls back to a default profile if the database is unavailable.
     *
     * @return A list of Profile objects.
     * @throws SQLException If database execution fails (and not intercepted by fallback).
     */
    @Override
    public List<Profile> getAllProfiles() throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles";
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                profiles.add(new Profile(
                        rs.getString("name"),
                        rs.getString("split_logic"),
                        parseSettings(rs.getString("settings")),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            // Fallback for offline mode: provide a standard scanning configuration
            profiles.add(getDefaultProfile());
        }
        return profiles;
    }

    /**
     * Creates a hardcoded default profile for use in offline mode.
     *
     * @return A default Profile object.
     */
    private Profile getDefaultProfile() {
        Map<String, String> settings = new HashMap<>();
        settings.put("rotation", "0");
        settings.put("quality", "high");
        return new Profile("Default Profile", "Default Profile", settings, "Standard scanning configuration");
    }

    /**
     * Adds a new scanning profile to the database.
     *
     * @param profile The Profile object to add.
     * @throws SQLException If database execution fails.
     */
    @Override
    public void addProfile(Profile profile) throws SQLException {
        String sql = "INSERT INTO profiles (name, split_logic, settings, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.getName());
            pstmt.setString(2, profile.getSplitLogic());
            pstmt.setString(3, serializeSettings(profile.getSettings()));
            pstmt.setString(4, profile.getDescription());
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a scanning profile from the database by name.
     *
     * @param name The name of the profile to delete.
     * @throws SQLException If database execution fails.
     */
    @Override
    public void deleteProfile(String name) throws SQLException {
        String sql = "DELETE FROM profiles WHERE name = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all profiles assigned to a specific user.
     * In offline mode, both 'admin' and 'user' are assigned the default profile.
     *
     * @param username The username of the user.
     * @return A list of Profile objects assigned to that user.
     * @throws SQLException If database execution fails (and not intercepted by fallback).
     */
    @Override
    public List<Profile> getUserProfiles(String username) throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT p.* FROM profiles p " +
                     "JOIN user_profiles up ON p.id = up.profile_id " +
                     "WHERE up.username = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    profiles.add(new Profile(
                            rs.getString("name"),
                            rs.getString("split_logic"),
                            parseSettings(rs.getString("settings")),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            // Fallback for offline mode: assign the default profile to system accounts
            if ("admin".equals(username) || "user".equals(username)) {
                profiles.add(getDefaultProfile());
            }
        }
        return profiles;
    }

    /**
     * Assigns a scanning profile to a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile to assign.
     * @throws SQLException If database execution fails.
     */
    @Override
    public void assignProfileToUser(String username, String profileName) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT 1 FROM user_profiles WHERE username = ? AND profile_id = (SELECT id FROM profiles WHERE name = ?)) " +
                     "INSERT INTO user_profiles (username, profile_id) " +
                     "SELECT ?, id FROM profiles WHERE name = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, profileName);
            pstmt.setString(3, username);
            pstmt.setString(4, profileName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Removes an assigned scanning profile from a user.
     *
     * @param username    The username of the user.
     * @param profileName The name of the profile to remove.
     * @throws SQLException If database execution fails.
     */
    @Override
    public void removeProfileFromUser(String username, String profileName) throws SQLException {
        String sql = "DELETE FROM user_profiles WHERE username = ? AND " +
                     "profile_id = (SELECT id FROM profiles WHERE name = ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, profileName);
            pstmt.executeUpdate();
        }
    }

    /**
     * Serializes a settings map into a semicolon-separated key-value string.
     *
     * @param map The map of settings to serialize.
     * @return The serialized settings string.
     */
    private String serializeSettings(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(escape(k)).append("=").append(escape(v)).append(";"));
        return sb.toString();
    }

    /**
     * Parses a semicolon-separated key-value string into a settings map.
     *
     * @param s The serialized settings string to parse.
     * @return A map containing key-value setting pairs.
     */
    private Map<String, String> parseSettings(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        String[] pairs = s.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(unescape(kv[0].trim()), unescape(kv[1].trim()));
            }
        }
        return map;
    }

    /**
     * Escapes special delimiter characters (';' and '=') in settings.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    private String escape(String s) {
        return (s == null) ? "" : s.replace(";", "%3B").replace("=", "%3D");
    }

    /**
     * Unescapes special delimiter characters (';' and '=') in settings.
     *
     * @param s The string to unescape.
     * @return The unescaped string.
     */
    private String unescape(String s) {
        return (s == null) ? "" : s.replace("%3B", ";").replace("%3D", "=");
    }
}
