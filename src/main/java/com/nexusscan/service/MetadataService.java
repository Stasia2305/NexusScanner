package com.nexusscan.service;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.IMetadataDAO;
import com.nexusscan.model.MetadataField;

import java.sql.SQLException;
import java.util.List;

public class MetadataService {
    private final IMetadataDAO metadataDAO;

    public MetadataService() {
        this.metadataDAO = DAOFactory.getMetadataDAO();
    }

    public List<MetadataField> getAllFields() throws SQLException {
        return metadataDAO.getAllFields();
    }

    public void addField(String name) throws SQLException {
        metadataDAO.addField(name);
    }

    public void deleteField(String name) throws SQLException {
        metadataDAO.deleteField(name);
    }
}
