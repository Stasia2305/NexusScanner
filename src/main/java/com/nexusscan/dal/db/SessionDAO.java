package com.nexusscan.dal.db;

import com.nexusscan.dal.interfaces.ISessionDAO;
import com.nexusscan.model.Document;
import com.nexusscan.model.Page;
import com.nexusscan.model.Profile;
import com.nexusscan.dal.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO implements ISessionDAO {
    private final DatabaseService databaseService;

    public SessionDAO() {
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    public void exportSession(Profile profile, String boxIdStr, String metadataStr, List<Document> documents) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int clientId = getOrCreateEntity(conn, "SELECT id FROM clients WHERE name = ?", "INSERT INTO clients (name) VALUES (?)", profile.getName());
                int archiveId = getOrCreateEntity(conn, "SELECT id FROM archives WHERE client_id = ? AND name = ?", "INSERT INTO archives (client_id, name) VALUES (?, ?)", clientId, "Main Archive");
                int boxId = getOrCreateEntity(conn, "SELECT id FROM boxes WHERE archive_id = ? AND box_id_str = ?", "INSERT INTO boxes (archive_id, box_id_str) VALUES (?, ?)", archiveId, boxIdStr);
                int caseId = getOrCreateEntity(conn, "SELECT id FROM cases WHERE box_id = ? AND case_number = ?", "INSERT INTO cases (box_id, case_number, metadata) VALUES (?, ?, ?)", boxId, "CASE-" + boxIdStr, metadataStr);

                if (clientId == -1 || archiveId == -1 || boxId == -1 || caseId == -1) {
                    throw new SQLException("Failed to create or retrieve hierarchy entities");
                }

                // Update metadata if case already existed
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE cases SET metadata = ? WHERE id = ?")) {
                    pstmt.setString(1, metadataStr);
                    pstmt.setInt(2, caseId);
                    pstmt.executeUpdate();
                }

                // Deduplicate by removing existing documents/pages for this case
                clearExistingCaseData(conn, caseId);

                // Insert new documents and pages
                insertDocumentsAndPages(conn, caseId, documents);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void clearExistingCaseData(Connection conn, int caseId) throws SQLException {
        List<Integer> oldDocIds = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM documents WHERE case_id = ?")) {
            pstmt.setInt(1, caseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    oldDocIds.add(rs.getInt(1));
                }
            }
        }
        
        try (PreparedStatement pstmtDelPages = conn.prepareStatement("DELETE FROM pages WHERE document_id = ?")) {
            for (int oldDocId : oldDocIds) {
                pstmtDelPages.setInt(1, oldDocId);
                pstmtDelPages.executeUpdate();
            }
        }
        
        try (PreparedStatement pstmtDelDocs = conn.prepareStatement("DELETE FROM documents WHERE case_id = ?")) {
            pstmtDelDocs.setInt(1, caseId);
            pstmtDelDocs.executeUpdate();
        }
    }

    private void insertDocumentsAndPages(Connection conn, int caseId, List<Document> documents) throws SQLException {
        String sqlDoc = "INSERT INTO documents (case_id, barcode, status) VALUES (?, ?, ?)";
        String sqlPage = "INSERT INTO pages (document_id, page_number, image_data, rotation) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmtDoc = conn.prepareStatement(sqlDoc, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtPage = conn.prepareStatement(sqlPage)) {
            for (Document doc : documents) {
                if (doc.getPages().isEmpty()) continue;

                pstmtDoc.setInt(1, caseId);
                pstmtDoc.setString(2, doc.getBarcode());
                pstmtDoc.setString(3, doc.getStatus().name());
                pstmtDoc.executeUpdate();

                try (ResultSet rs = pstmtDoc.getGeneratedKeys()) {
                    if (rs.next()) {
                        int docId = rs.getInt(1);
                        for (Page page : doc.getPages()) {
                            pstmtPage.setInt(1, docId);
                            pstmtPage.setInt(2, page.getPageNumber());
                            pstmtPage.setBytes(3, page.getImageData());
                            pstmtPage.setDouble(4, page.getRotation());
                            pstmtPage.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private int getOrCreateEntity(Connection conn, String selectSql, String insertSql, Object... params) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            for (int i = 0; i < countPlaceholders(selectSql); i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') count++;
        }
        return count;
    }
}
