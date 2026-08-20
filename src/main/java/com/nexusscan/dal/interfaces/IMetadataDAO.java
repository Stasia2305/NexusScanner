package com.nexusscan.dal.interfaces;

import com.nexusscan.model.MetadataField;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Data Access Objects managing metadata fields.
 */
public interface IMetadataDAO {
    /**
     * Retrieves all metadata fields defined in the database.
     *
     * @return A list of all available MetadataField objects.
     * @throws SQLException If a database error occurs.
     */
    List<MetadataField> getAllFields() throws SQLException;

    /**
     * Adds a new metadata field definition to the database.
     *
     * @param name The name of the new metadata field.
     * @throws SQLException If a database error occurs.
     */
    void addField(String name) throws SQLException;

    /**
     * Deletes a metadata field definition from the database.
     *
     * @param name The name of the metadata field to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteField(String name) throws SQLException;
}
