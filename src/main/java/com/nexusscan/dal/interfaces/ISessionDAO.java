package com.nexusscan.dal.interfaces;

import com.nexusscan.model.Document;
import com.nexusscan.model.Profile;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface for saving and retrieving scanning sessions.
 */
public interface ISessionDAO {
    /**
     * Exports a scanning session to the database.
     * @param profile The profile used for scanning.
     * @param boxIdStr The identifier for the physical box.
     * @param metadataStr Serialized metadata for the session.
     * @param documents The list of documents and pages to save.
     * @throws SQLException If a database error occurs.
     */
    void exportSession(Profile profile, String boxIdStr, String metadataStr, List<Document> documents) throws SQLException;
}
