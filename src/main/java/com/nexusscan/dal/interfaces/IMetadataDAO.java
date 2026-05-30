package com.nexusscan.dal.interfaces;

import com.nexusscan.model.MetadataField;
import java.sql.SQLException;
import java.util.List;

public interface IMetadataDAO {
    List<MetadataField> getAllFields() throws SQLException;
    void addField(String name) throws SQLException;
    void deleteField(String name) throws SQLException;
}
