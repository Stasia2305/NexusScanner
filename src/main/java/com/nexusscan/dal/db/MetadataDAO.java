package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.IMetadataDAO;
import com.nexusscan.model.MetadataField;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server implementation for managing metadata fields.
 */
public class MetadataDAO implements IMetadataDAO {
    private final DatabaseService databaseService;

    /**
     * Constructs a new MetadataDAO and obtains the database service instance.
     */
    public MetadataDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    /**
     * Retrieves all defined metadata fields.
     * Provides default fields (Case ID, Client Name, Document Type) in offline mode.
     *
     * @return A list of MetadataField objects.
     * @throws SQLException If database execution fails.
     */
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
        } catch (SQLException e) {
            // Fallback for offline mode: ensure the UI has basic fields to work with
            fields.add(new MetadataField(1, "Case ID"));
            fields.add(new MetadataField(2, "Client Name"));
            fields.add(new MetadataField(3, "Document Type"));
        }
        return fields;
    }

    /**
     * Adds a new metadata field definition.
     *
     * @param name The name of the new metadata field.
     * @throws SQLException If database execution fails.
     */
    @Override
    public void addField(String name) throws SQLException {
        String sql = "INSERT INTO metadata_fields (field_name) VALUES (?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a metadata field definition.
     *
     * @param name The name of the metadata field to delete.
     * @throws SQLException If database execution fails.
     */
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
