package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.IMetadataDAO;
import com.nexusscan.model.MetadataField;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class for managing metadata fields and structures.
 */
public class MetadataService {
    private final IMetadataDAO metadataDAO;

    /**
     * Constructs a new MetadataService and obtains the IMetadataDAO implementation.
     */
    public MetadataService() {
        this.metadataDAO = DAOFactory.getMetadataDAO();
    }

    /**
     * Retrieves all metadata fields.
     *
     * @return A list of MetadataField objects.
     * @throws SQLException If a database error occurs.
     */
    public List<MetadataField> getAllFields() throws SQLException {
        return metadataDAO.getAllFields();
    }

    /**
     * Adds a new metadata field definition.
     *
     * @param name The name of the new field.
     * @throws SQLException If a database error occurs.
     */
    public void addField(String name) throws SQLException {
        metadataDAO.addField(name);
    }

    /**
     * Deletes a metadata field definition.
     *
     * @param name The name of the field to delete.
     * @throws SQLException If a database error occurs.
     */
    public void deleteField(String name) throws SQLException {
        metadataDAO.deleteField(name);
    }
}
