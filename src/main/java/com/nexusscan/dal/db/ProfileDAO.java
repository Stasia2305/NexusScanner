package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.IProfileDAO;
import com.nexusscan.model.Profile;
import com.nexusscan.service.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileDAO implements IProfileDAO {
    private final DatabaseService databaseService;

    public ProfileDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

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
        }
        return profiles;
    }

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

    @Override
    public void deleteProfile(String name) throws SQLException {
        String sql = "DELETE FROM profiles WHERE name = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

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
        }
        return profiles;
    }

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

    private String serializeSettings(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(escape(k)).append("=").append(escape(v)).append(";"));
        return sb.toString();
    }

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

    private String escape(String s) {
        return (s == null) ? "" : s.replace(";", "%3B").replace("=", "%3D");
    }

    private String unescape(String s) {
        return (s == null) ? "" : s.replace("%3B", ";").replace("%3D", "=");
    }
}
