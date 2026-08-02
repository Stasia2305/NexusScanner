package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.IMetadataDAO;
import com.nexusscan.model.MetadataField;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetadataDAO implements IMetadataDAO {
    private final DatabaseService databaseService;

    public MetadataDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    public List<MetadataField> getAllFields() throws SQLException {
        List<MetadataField> fields = new ArrayList<>();
        String sql = "SELECT * FROM metadata_fields";
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                fields.add(new MetadataField(rs.getInt("id"), rs.getString("field_name")));
            }
        }
        return fields;
    }

    @Override
    public void addField(String name) throws SQLException {
        String sql = "INSERT INTO metadata_fields (field_name) VALUES (?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteField(String name) throws SQLException {
        String sql = "DELETE FROM metadata_fields WHERE field_name = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }
}
