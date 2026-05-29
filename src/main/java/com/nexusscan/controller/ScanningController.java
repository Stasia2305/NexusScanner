package com.nexusscan.controller;

import com.nexusscan.model.*;
import com.nexusscan.service.AppState;
import com.nexusscan.service.LoggingService;
import com.nexusscan.service.ScanService;
import com.nexusscan.service.DatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.layout.GridPane;

/**
 * Controller for the main scanning workspace.
 * Manages the scanning session, document tree visualization, image manipulation, and database persistence.
 */
public class ScanningController {
    @FXML private Label profileLabel;
    @FXML private Label boxLabel;
    @FXML private Label totalScansLabel;
    @FXML private TreeView<Object> fileTreeView;
    @FXML private ImageView fileImageView;
    @FXML private Button scanButton;
    @FXML private Button qaButton;

    private Profile currentProfile;
    private String currentBoxId;
    private List<Document> documents = new ArrayList<>();
    private int totalScans = 0;
    private Page currentFile;
    private double globalRotation = 0;
    private boolean isQAMode = false;
    private String metadataStr = "";

    /**
     * Initializes the scanning session with the selected profile and box identifier.
     */
    public void setSession(Profile profile, String boxId) {
        this.currentProfile = profile;
        this.currentBoxId = boxId;
        profileLabel.setText("Profile: " + profile.getName());
        boxLabel.setText("Box: " + boxId);
        
        // Initialize with a starting document
        documents.add(new Document(1, "START"));
        refreshTreeView();

        Platform.runLater(this::setupShortcuts);
    }

    /**
     * Configures keyboard shortcuts for fast operation (F1 for Scan, F12 for Export, etc.).
     */
    private void setupShortcuts() {
        Scene scene = profileLabel.getScene();
        if (scene == null) return;

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F1) {
                onScanClick();
                event.consume();
            } else if (event.getCode() == KeyCode.F12) {
                onExportClick();
                event.consume();
            } else if (event.getCode() == KeyCode.L) {
                onRotateLeftClick();
                event.consume();
            } else if (event.getCode() == KeyCode.R) {
                onRotateRightClick();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                if (!fileTreeView.isFocused()) {
                    onPrevClick();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.RIGHT) {
                if (!fileTreeView.isFocused()) {
                    onNextClick();
                    event.consume();
                }
            }
        });
    }

    @FXML
    public void initialize() {
        // Custom cell factory for TreeView to style Documents and Pages differently
        fileTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); 
                } else if (item instanceof Document) {
                    Document doc = (Document) item;
                    setText("Document: " + doc.getBarcode() + (isQAMode ? " [" + doc.getStatus() + "]" : ""));
                    setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                } else if (item instanceof Page) {
                    setText("Page: " + ((Page) item).getPageNumber());
                    setStyle("-fx-text-fill: brown;");
                }
            }
        });

        // Update image display when a page is selected in the tree
        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof Page) {
                displayFile((Page) newVal.getValue());
            } else {
                clearDisplay();
            }
        });
    }

    private void clearDisplay() {
        currentFile = null;
        fileImageView.setImage(null);
    }

    /**
     * Triggers the scanning process. Handles barcode detection and profile-based auto-splitting.
     */
    @FXML
    private void onScanClick() {
        if (isQAMode || currentProfile == null) return;
        ScanService.ScanResult result = ScanService.getInstance().scan();
        
        // Check for numeric split logic (e.g., split every N pages)
        boolean fixedSplitTriggered = false;
        String splitLogic = currentProfile.getSplitLogic();
        if (splitLogic != null && !splitLogic.isEmpty()) {
            try {
                int interval = Integer.parseInt(splitLogic.trim());
                if (interval > 0 && totalScans > 0 && totalScans % interval == 0 && !result.isBarcode()) {
                    fixedSplitTriggered = true;
                }
            } catch (NumberFormatException e) {
                // Not a numeric interval, ignore
            }
        }

        // Split document if barcode detected or auto-split interval reached
        if (result.isBarcode() || fixedSplitTriggered) {
            String barcodeVal = result.isBarcode() ? result.getImagePath() : "AUTO-SPLIT-" + (documents.size() + 1);
            documents.add(new Document(totalScans + 1, barcodeVal));
            LoggingService.getInstance().log("Document split triggered", AppState.getInstance().getCurrentUsernameSafe());
            
            if (result.isBarcode()) {
                // Prompt user when a barcode is scanned
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Barcode Detected");
                alert.setHeaderText("A barcode was detected, splitting document.");
                alert.setContentText("What would you like to do?");

                ButtonType stopBtn = new ButtonType("Stop & Enter Metadata");
                ButtonType continueBtn = new ButtonType("Continue Scanning");
                alert.getButtonTypes().setAll(stopBtn, continueBtn);

                alert.showAndWait().ifPresent(type -> {
                    if (type == stopBtn) {
                        onMetadataClick();
                    }
                });
            } else {
                // For auto-splits based on page count, also process the current page
                processScannedPage(result);
            }
        } else {
            processScannedPage(result);
        }
        
        refreshTreeView();
    }

    /**
     * Processes a successfully scanned page, applying default rotations and metadata.
     */
    private void processScannedPage(ScanService.ScanResult result) {
        totalScans++;
        totalScansLabel.setText("Total Scans: " + totalScans);
        
        Page page = new Page(totalScans, totalScans, result.getImagePath());
        
        // Apply initial rotation from profile settings
        double initialRotation = globalRotation;
        String profileRotation = currentProfile.getSetting("rotation", null);
        if (profileRotation != null) {
            try {
                initialRotation += Double.parseDouble(profileRotation);
            } catch (NumberFormatException e) {
                // Ignore invalid setting
            }
        }
        
        page.setRotation(initialRotation);
        page.setImageData(result.getData());
        
        // Ensure we have at least one document to add the page to
        if (documents.isEmpty()) documents.add(new Document(1, "START"));
        documents.get(documents.size() - 1).addPage(page);
        
        LoggingService.getInstance().log("User scanned page: " + totalScans, AppState.getInstance().getCurrentUsernameSafe());
    }

    @FXML
    private void onMetadataClick() {
        List<MetadataField> fields = AppState.getInstance().getMetadataFields();
        if (fields.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No metadata fields defined by admin.").show();
            return;
        }

        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Metadata Entry");
        dialog.setHeaderText("Enter metadata for the current case");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        Map<String, TextField> textFields = new HashMap<>();
        // Parse existing metadataStr if possible to pre-fill
        Map<String, String> existingValues = parseMetadata(metadataStr);

        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i).getFieldName();
            grid.add(new Label(fieldName + ":"), 0, i);
            TextField tf = new TextField();
            tf.setText(existingValues.getOrDefault(fieldName, ""));
            grid.add(tf, 1, i);
            textFields.put(fieldName, tf);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, String> results = new HashMap<>();
                textFields.forEach((k, v) -> results.put(k, v.getText()));
                return results;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(results -> {
            this.metadataStr = serializeMetadata(results);
            LoggingService.getInstance().log("Metadata updated: " + metadataStr, AppState.getInstance().getCurrentUsernameSafe());
        });
    }

    private Map<String, String> parseMetadata(String meta) {
        Map<String, String> map = new HashMap<>();
        if (meta == null || meta.isEmpty()) return map;
        String[] pairs = meta.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private String serializeMetadata(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
        return sb.toString();
    }

    /**
     * Toggles Quality Assurance (QA) mode, allowing review of scanned documents.
     */
    @FXML
    private void onQAModeClick() {
        isQAMode = !isQAMode;
        qaButton.setText(isQAMode ? "Exit QA Mode" : "QA Mode");
        scanButton.setDisable(isQAMode);
        if (isQAMode) {
            for (Document doc : documents) {
                if (doc.getStatus() == Document.Status.IN_PROGRESS) {
                    doc.setStatus(Document.Status.WAITING_FOR_QA);
                }
            }
        }
        refreshTreeView();
    }

    private void refreshTreeView() {
        TreeItem<Object> root = new TreeItem<>("Root");
        for (Document doc : documents) {
            TreeItem<Object> docItem = new TreeItem<>(doc);
            docItem.setExpanded(true);
            for (Page page : doc.getPages()) {
                docItem.getChildren().add(new TreeItem<>(page));
            }
            root.getChildren().add(docItem);
        }
        fileTreeView.setRoot(root);
        fileTreeView.setShowRoot(false);
    }

    private void selectFileInTreeView(Page file) {
        for (TreeItem<Object> docItem : fileTreeView.getRoot().getChildren()) {
            for (TreeItem<Object> fileItem : docItem.getChildren()) {
                if (fileItem.getValue() == file) {
                    fileTreeView.getSelectionModel().select(fileItem);
                    return;
                }
            }
        }
    }

    private void displayFile(Page file) {
        currentFile = file;
        Image image = new Image(file.getImagePath(), true);
        fileImageView.setImage(image);
        fileImageView.setRotate(file.getRotation());
    }

    @FXML private void onRotateLeftClick() { 
        if (currentFile != null) { 
            currentFile.setRotation(currentFile.getRotation() - 90); 
            fileImageView.setRotate(currentFile.getRotation()); 
        } 
    }
    
    @FXML private void onRotateRightClick() { 
        if (currentFile != null) { 
            currentFile.setRotation(currentFile.getRotation() + 90); 
            fileImageView.setRotate(currentFile.getRotation()); 
        } 
    }

    /**
     * Reorders a page by moving it up within its parent document in the TreeView.
     */
    @FXML
    private void onMoveUpClick() {
        TreeItem<Object> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof Page) {
            TreeItem<Object> parent = selectedItem.getParent();
            int index = parent.getChildren().indexOf(selectedItem);
            if (index > 0) {
                // Remove and re-insert at previous position
                parent.getChildren().remove(selectedItem);
                parent.getChildren().add(index - 1, selectedItem);
                fileTreeView.getSelectionModel().select(selectedItem);
                // Synchronize the internal model with the new tree structure
                updateInternalModelFromTree();
            }
        }
    }

    /**
     * Reorders a page by moving it down within its parent document in the TreeView.
     */
    @FXML
    private void onMoveDownClick() {
        TreeItem<Object> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof Page) {
            TreeItem<Object> parent = selectedItem.getParent();
            int index = parent.getChildren().indexOf(selectedItem);
            if (index < parent.getChildren().size() - 1) {
                // Remove and re-insert at next position
                parent.getChildren().remove(selectedItem);
                parent.getChildren().add(index + 1, selectedItem);
                fileTreeView.getSelectionModel().select(selectedItem);
                // Synchronize the internal model with the new tree structure
                updateInternalModelFromTree();
            }
        }
    }

    /**
     * Moves a selected page into the subsequent document in the tree.
     */
    @FXML
    private void onMoveDocClick() {
        TreeItem<Object> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof Page) {
            TreeItem<Object> parent = selectedItem.getParent();
            TreeItem<Object> root = parent.getParent();
            int docIndex = root.getChildren().indexOf(parent);
            
            // Attempt to move to the next document in the hierarchy
            if (docIndex < root.getChildren().size() - 1) {
                TreeItem<Object> nextDoc = root.getChildren().get(docIndex + 1);
                parent.getChildren().remove(selectedItem);
                nextDoc.getChildren().add(0, selectedItem);
                fileTreeView.getSelectionModel().select(selectedItem);
                // Synchronize the internal model with the new tree structure
                updateInternalModelFromTree();
            }
        }
    }

    /**
     * Syncs the internal 'documents' list with the current visual state of the TreeView.
     * This is called after any drag-and-drop or reordering operation.
     */
    private void updateInternalModelFromTree() {
        TreeItem<Object> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        Object selectedValue = (selectedItem != null) ? selectedItem.getValue() : null;

        documents.clear();
        int globalPageCounter = 1;
        // Traverse the tree structure to reconstruct the list hierarchy
        for (TreeItem<Object> docItem : fileTreeView.getRoot().getChildren()) {
            Document doc = (Document) docItem.getValue();
            doc.getPages().clear();
            for (TreeItem<Object> fileItem : docItem.getChildren()) {
                Page file = (Page) fileItem.getValue();
                // Update page numbers based on new order
                file.setPageNumber(globalPageCounter++);
                doc.addPage(file);
            }
            documents.add(doc);
        }
        refreshTreeView();
        
        // Restore selection
        if (selectedValue != null) {
            restoreSelection(selectedValue);
        }
    }

    private void restoreSelection(Object value) {
        for (TreeItem<Object> docItem : fileTreeView.getRoot().getChildren()) {
            if (docItem.getValue().equals(value)) {
                fileTreeView.getSelectionModel().select(docItem);
                return;
            }
            for (TreeItem<Object> fileItem : docItem.getChildren()) {
                if (fileItem.getValue().equals(value)) {
                    fileTreeView.getSelectionModel().select(fileItem);
                    return;
                }
            }
        }
    }

    @FXML
    private void onPrevClick() {
        List<Page> allFiles = getAllFiles();
        int index = allFiles.indexOf(currentFile);
        if (index > 0) {
            selectFileInTreeView(allFiles.get(index - 1));
        }
    }

    @FXML
    private void onNextClick() {
        List<Page> allFiles = getAllFiles();
        int index = allFiles.indexOf(currentFile);
        if (index >= 0 && index < allFiles.size() - 1) {
            selectFileInTreeView(allFiles.get(index + 1));
        }
    }

    private List<Page> getAllFiles() {
        List<Page> allFiles = new ArrayList<>();
        for (Document doc : documents) {
            allFiles.addAll(doc.getPages());
        }
        return allFiles;
    }

    @FXML
    private void onSettingsClick() {
        ChoiceDialog<Double> dialog = new ChoiceDialog<>(globalRotation, 0.0, 90.0, 180.0, 270.0);
        dialog.setTitle("Settings");
        dialog.setHeaderText("Set Global Rotation for Box");
        dialog.setContentText("Rotation:");
        dialog.showAndWait().ifPresent(rot -> {
            globalRotation = rot;
            // P2 Fix: Apply to all existing files
            for (Document doc : documents) {
                for (Page f : doc.getPages()) {
                    f.setRotation(rot);
                }
            }
            if (currentFile != null) fileImageView.setRotate(rot);
            LoggingService.getInstance().log("Global rotation set to " + rot, AppState.getInstance().getCurrentUsernameSafe());
        });
    }

    /**
     * Saves the current scanning session to the database and marks documents as QA completed.
     */
    @FXML
    private void onExportClick() {
        try {
            // P1 Fix: Update status BEFORE saving to DB
            for (Document doc : documents) {
                if (!doc.getPages().isEmpty()) {
                    doc.setStatus(Document.Status.QA_COMPLETED);
                }
            }
            saveToDatabase();
            String exportName = currentProfile.getName() + "_" + currentBoxId;
            Alert info = new Alert(Alert.AlertType.INFORMATION, "Data saved to database successfully! Status marked as QA Completed. Export Name: " + exportName);
            info.show();
            refreshTreeView();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage()).show();
        }
    }

    /**
     * Persists the entire hierarchy (Client, Archive, Box, Case, Document, Page) to SQLite.
     * Uses transactions to ensure data integrity.
     */
    private void saveToDatabase() throws SQLException {
        DatabaseService db = DatabaseService.getInstance();
        Connection conn = db.getConnection();
        conn.setAutoCommit(false); // Use transaction for integrity

        try {
            int clientId = getOrCreateEntity(conn, "SELECT id FROM clients WHERE name = ?", "INSERT INTO clients (name) VALUES (?)", currentProfile.getName());
            int archiveId = getOrCreateEntity(conn, "SELECT id FROM archives WHERE client_id = ? AND name = ?", "INSERT INTO archives (client_id, name) VALUES (?, ?)", clientId, "Main Archive");
            int boxId = getOrCreateEntity(conn, "SELECT id FROM boxes WHERE archive_id = ? AND box_id_str = ?", "INSERT INTO boxes (archive_id, box_id_str) VALUES (?, ?)", archiveId, currentBoxId);
            int caseId = getOrCreateEntity(conn, "SELECT id FROM cases WHERE box_id = ? AND case_number = ?", "INSERT INTO cases (box_id, case_number, metadata) VALUES (?, ?, ?)", boxId, "CASE-" + currentBoxId, metadataStr);

            if (clientId == -1 || archiveId == -1 || boxId == -1 || caseId == -1) {
                throw new SQLException("Failed to create or retrieve hierarchy entities");
            }

            // P2 Fix: Update metadata if case already existed
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE cases SET metadata = ? WHERE id = ?")) {
                pstmt.setString(1, metadataStr);
                pstmt.setInt(2, caseId);
                pstmt.executeUpdate();
            }

            // P2 Fix: Deduplicate by removing existing documents/pages for this case before re-exporting
            // We find all documents for this case and delete their pages, then delete the documents
            try (PreparedStatement pstmtGetDocs = conn.prepareStatement("SELECT id FROM documents WHERE case_id = ?")) {
                pstmtGetDocs.setInt(1, caseId);
                try (ResultSet rs = pstmtGetDocs.executeQuery()) {
                    while (rs.next()) {
                        int oldDocId = rs.getInt(1);
                        try (PreparedStatement pstmtDelPages = conn.prepareStatement("DELETE FROM pages WHERE document_id = ?")) {
                            pstmtDelPages.setInt(1, oldDocId);
                            pstmtDelPages.executeUpdate();
                        }
                    }
                }
            }
            try (PreparedStatement pstmtDelDocs = conn.prepareStatement("DELETE FROM documents WHERE case_id = ?")) {
                pstmtDelDocs.setInt(1, caseId);
                pstmtDelDocs.executeUpdate();
            }

            String sqlDoc = "INSERT INTO documents (case_id, barcode, status) VALUES (?, ?, ?)";
            String sqlPage = "INSERT INTO pages (document_id, page_number, image_data, rotation) VALUES (?, ?, ?, ?)";

            for (Document doc : documents) {
                if (doc.getPages().isEmpty()) continue; // Skip empty documents (P2 Fix)

                try (PreparedStatement pstmtDoc = conn.prepareStatement(sqlDoc, Statement.RETURN_GENERATED_KEYS)) {
                    pstmtDoc.setInt(1, caseId);
                    pstmtDoc.setString(2, doc.getBarcode());
                    pstmtDoc.setString(3, doc.getStatus().name());
                    pstmtDoc.executeUpdate();

                    try (ResultSet rs = pstmtDoc.getGeneratedKeys()) {
                        if (rs.next()) {
                            int docId = rs.getInt(1);
                            for (Page page : doc.getPages()) {
                                try (PreparedStatement pstmtPage = conn.prepareStatement(sqlPage)) {
                                    pstmtPage.setInt(1, docId);
                                    pstmtPage.setInt(2, page.getPageNumber());
                                    pstmtPage.setBytes(3, page.getImageData()); // Save BLOB
                                    pstmtPage.setDouble(4, page.getRotation());
                                    pstmtPage.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
            conn.commit();
            LoggingService.getInstance().log("Full hierarchy and pages saved to database. Metadata: " + metadataStr, AppState.getInstance().getCurrentUsernameSafe());
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private int getOrCreateEntity(Connection conn, String selectSql, String insertSql, Object... params) throws SQLException {
        // Try to find existing
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            int paramCount = countPlaceholders(selectSql);
            for (int i = 0; i < paramCount; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // Not found, insert
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

    @FXML
    private void onLogoutClick() throws IOException {
        if (!documents.isEmpty() && totalScans > 0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Exit");
            alert.setHeaderText("Unsaved scanning session");
            alert.setContentText("You have scanned pages that are not exported. Are you sure you want to exit? All unsaved work will be lost.");
            
            ButtonType exitBtn = new ButtonType("Exit and Discard", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(exitBtn, cancelBtn);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancelBtn) {
                return;
            }
        }

        AppState.getInstance().setCurrentUser(null); // P2 Fix: Clear session
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = (Stage) profileLabel.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Login");
    }
}
