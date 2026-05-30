package com.nexusscan.controller;

import com.nexusscan.model.*;
import com.nexusscan.service.AppState;
import com.nexusscan.service.LoggingService;
import com.nexusscan.service.ScanService;
import com.nexusscan.service.DatabaseService;
import com.nexusscan.service.strategy.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
 * The ScanningController handles the main scanning workspace where users process documents.
 * It manages the scanning process, organizes scanned pages into documents, allows for 
 * image rotation, metadata entry, and eventually saving everything to the database.
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
    private ISplitStrategy splitStrategy;

    /**
     * Prepares the scanning screen for a new session.
     * Sets the profile being used and the box ID being scanned.
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
     * Sets up keyboard shortcuts to speed up work.
     * F1: Start Scan
     * F12: Export Session
     * L/R: Rotate image Left/Right
     * Arrow Keys: Navigate between pages
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
        // Initialize splitting strategy (Composite Pattern)
        CompositeSplitStrategy composite = new CompositeSplitStrategy();
        composite.addStrategy(new BarcodeSplitStrategy());
        composite.addStrategy(new FixedPageSplitStrategy());
        this.splitStrategy = composite;

        // Define how documents and pages look in the sidebar list (TreeView)
        fileTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); 
                } else if (item instanceof Document) {
                    // Documents are shown in bold blue text
                    Document doc = (Document) item;
                    setText("Document: " + doc.getBarcode() + (isQAMode ? " [" + doc.getStatus() + "]" : ""));
                    setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                } else if (item instanceof Page) {
                    // Pages are shown in brown text
                    setText("Page: " + ((Page) item).getPageNumber());
                    setStyle("-fx-text-fill: brown;");
                }
            }
        });

        // Show the image when a page is clicked in the list
        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof Page) {
                displayFile((Page) newVal.getValue());
            } else {
                clearDisplay();
            }
        });
    }

    /**
     * Clears the image viewer when nothing is selected.
     */
    private void clearDisplay() {
        currentFile = null;
        fileImageView.setImage(null);
    }

    /**
     * Starts the scanning process. 
     * Handles splitting documents based on barcodes or a set number of pages.
     */
    @FXML
    private void onScanClick() {
        if (isQAMode || currentProfile == null) return;
        
        scanButton.setDisable(true);
        Task<ScanService.ScanResult> scanTask = new Task<>() {
            @Override
            protected ScanService.ScanResult call() {
                return ScanService.getInstance().scan();
            }
        };

        scanTask.setOnSucceeded(e -> {
            scanButton.setDisable(false);
            ScanService.ScanResult result = scanTask.getValue();
            handleScanResult(result);
        });

        scanTask.setOnFailed(e -> {
            scanButton.setDisable(false);
            new Alert(Alert.AlertType.ERROR, "Scanning failed: " + scanTask.getException().getMessage()).show();
        });

        new Thread(scanTask).start();
    }

    private void handleScanResult(ScanService.ScanResult result) {
        // Use the Strategy Pattern to determine if a split should occur
        if (splitStrategy.shouldSplit(result, totalScans, currentProfile.getSplitLogic())) {
            String barcodeVal = result.isBarcode() ? result.getImagePath() : "AUTO-SPLIT-" + (documents.size() + 1);
            documents.add(new Document(totalScans + 1, barcodeVal));
            LoggingService.getInstance().log("Document split triggered by strategy", AppState.getInstance().getCurrentUsernameSafe());
            
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
     * Handles the data for a single scanned page.
     * Applies rotation settings and adds it to the current document.
     */
    private void processScannedPage(ScanService.ScanResult result) {
        totalScans++;
        totalScansLabel.setText("Total Scans: " + totalScans);
        
        Page page = new Page(totalScans, totalScans, result.getImagePath());
        
        // Use the rotation from the profile settings as the starting point
        double initialRotation = globalRotation;
        String profileRotation = currentProfile.getSetting("rotation", null);
        if (profileRotation != null) {
            try {
                initialRotation += Double.parseDouble(profileRotation);
            } catch (NumberFormatException e) {
                // Ignore if the setting is not a valid number
            }
        }
        
        page.setRotation(initialRotation);
        page.setImageData(result.getData());
        
        // Create a document if one doesn't exist yet
        if (documents.isEmpty()) documents.add(new Document(1, "START"));
        documents.get(documents.size() - 1).addPage(page);
        
        LoggingService.getInstance().log("User scanned page: " + totalScans, AppState.getInstance().getCurrentUsernameSafe());
    }

    /**
     * Opens a dialog for the user to enter metadata (like Case ID or Client Name).
     */
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

    /**
     * Converts a metadata string (key=value;key=value) into a Map.
     */
    private Map<String, String> parseMetadata(String meta) {
        Map<String, String> map = new HashMap<>();
        if (meta == null || meta.isEmpty()) return map;
        String[] pairs = meta.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(unescape(kv[0].trim()), unescape(kv[1].trim()));
            }
        }
        return map;
    }

    /**
     * Converts a Map of metadata into a single string (key=value;key=value).
     */
    private String serializeMetadata(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(escape(k)).append("=").append(escape(v)).append(";"));
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace(";", "%3B").replace("=", "%3D");
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("%3B", ";").replace("%3D", "=");
    }

    /**
     * Switches between Scanning mode and Quality Assurance (QA) mode.
     * QA mode allows reviewing documents before they are finalized.
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

    /**
     * Rebuilds the sidebar list (TreeView) to reflect the current documents and pages.
     */
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

    /**
     * Highlights a specific page in the sidebar list.
     */
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

    /**
     * Displays the scanned image of a page in the viewer.
     * It handles both standard web images and TIFF data from the API.
     */
    private void displayFile(Page file) {
        currentFile = file;
        
        // Prefer raw image data (like TIFFs from the API)
        if (file.getImageData() != null && file.getImageData().length > 0) {
            Task<Image> loadTask = new Task<>() {
                @Override
                protected Image call() throws IOException {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(file.getImageData())) {
                        BufferedImage bufferedImage = ImageIO.read(bis);
                        if (bufferedImage != null) {
                            return SwingFXUtils.toFXImage(bufferedImage, null);
                        }
                    }
                    return new Image(file.getImagePath(), true);
                }
            };

            loadTask.setOnSucceeded(e -> {
                if (currentFile == file) { // Only update if still viewing the same page
                    fileImageView.setImage(loadTask.getValue());
                    fileImageView.setRotate(file.getRotation());
                }
            });

            loadTask.setOnFailed(e -> {
                if (currentFile == file) {
                    fileImageView.setImage(new Image(file.getImagePath(), true));
                    fileImageView.setRotate(file.getRotation());
                }
            });

            new Thread(loadTask).start();
        } else {
            // No raw data, just load from the path (like the fallback picsum URLs)
            Image image = new Image(file.getImagePath(), true);
            fileImageView.setImage(image);
            fileImageView.setRotate(file.getRotation());
        }
    }

    /**
     * Rotates the current page 90 degrees to the left.
     */
    @FXML private void onRotateLeftClick() { 
        if (currentFile != null) { 
            currentFile.setRotation(currentFile.getRotation() - 90); 
            fileImageView.setRotate(currentFile.getRotation()); 
        } 
    }
    
    /**
     * Rotates the current page 90 degrees to the right.
     */
    @FXML private void onRotateRightClick() { 
        if (currentFile != null) { 
            currentFile.setRotation(currentFile.getRotation() + 90); 
            fileImageView.setRotate(currentFile.getRotation()); 
        } 
    }

    /**
     * Moves the selected page up by one position in the document.
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
     * Moves the selected page down by one position in the document.
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
     * Moves the selected page into the next document in the list.
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
     * Synchronizes the internal list of documents with what's currently shown in the sidebar list.
     * This ensures that any reordering done by the user is correctly saved.
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

    /**
     * Selects the previous page in the list.
     */
    @FXML
    private void onPrevClick() {
        List<Page> allFiles = getAllFiles();
        int index = allFiles.indexOf(currentFile);
        if (index > 0) {
            selectFileInTreeView(allFiles.get(index - 1));
        }
    }

    /**
     * Selects the next page in the list.
     */
    @FXML
    private void onNextClick() {
        List<Page> allFiles = getAllFiles();
        int index = allFiles.indexOf(currentFile);
        if (index >= 0 && index < allFiles.size() - 1) {
            selectFileInTreeView(allFiles.get(index + 1));
        }
    }

    /**
     * Gets a flat list of all pages across all documents.
     */
    private List<Page> getAllFiles() {
        List<Page> allFiles = new ArrayList<>();
        for (Document doc : documents) {
            allFiles.addAll(doc.getPages());
        }
        return allFiles;
    }

    /**
     * Opens a settings dialog to set a global rotation for all pages in this box.
     */
    @FXML
    private void onSettingsClick() {
        ChoiceDialog<Double> dialog = new ChoiceDialog<>(globalRotation, 0.0, 90.0, 180.0, 270.0);
        dialog.setTitle("Settings");
        dialog.setHeaderText("Set Global Rotation for Box");
        dialog.setContentText("Rotation:");
        dialog.showAndWait().ifPresent(rot -> {
            globalRotation = rot;
            // Apply the new rotation to every page already scanned
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
     * Saves the entire scanning session to the database.
     * All documents are marked as "QA Completed" during this process.
     */
    @FXML
    private void onExportClick() {
        if (scanButton.isDisable()) {
            new Alert(Alert.AlertType.WARNING, "Please wait for the current scan to finish.").show();
            return;
        }
        if (getAllFiles().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cannot export an empty session. Please scan pages first.").show();
            return;
        }
        try {
            // Mark every document that has pages as ready for finalization
            for (Document doc : documents) {
                if (!doc.getPages().isEmpty()) {
                    doc.setStatus(Document.Status.QA_COMPLETED);
                }
            }
            
            ScanService.getInstance().exportSession(currentProfile, currentBoxId, metadataStr, documents);
            
            String exportName = currentProfile.getName() + "_" + currentBoxId;
            new Alert(Alert.AlertType.INFORMATION, "Data saved to database successfully! Status marked as QA Completed. Export Name: " + exportName).show();
            refreshTreeView();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage()).show();
        }
    }

    /**
     * Highlights a specific page in the sidebar list.
     */

    @FXML
    private void onBackClick() throws IOException {
        if (scanButton.isDisable()) {
            new Alert(Alert.AlertType.WARNING, "Please wait for the current scan to finish.").show();
            return;
        }
        if (!documents.isEmpty() && totalScans > 0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Return");
            alert.setHeaderText("Unsaved scanning session");
            alert.setContentText("You have scanned pages that are not exported. Are you sure you want to go back? All unsaved work will be lost.");
            
            ButtonType backBtn = new ButtonType("Back and Discard", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(backBtn, cancelBtn);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancelBtn) {
                return;
            }
        }

        String fxml = AppState.getInstance().getCurrentUser().getRole() == User.Role.ADMIN ? "admin-dashboard.fxml" : "user-dashboard.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/" + fxml));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = (Stage) profileLabel.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(AppState.getInstance().getCurrentUser().getRole() == User.Role.ADMIN ? "Admin Dashboard" : "User Dashboard");
        stage.setMaximized(true);
        stage.setResizable(true);
    }

    /**
     * Logs out the current user and returns to the login screen.
     * Warns the user if there is unsaved work.
     */
    @FXML
    private void onLogoutClick() throws IOException {
        if (scanButton.isDisable()) {
            new Alert(Alert.AlertType.WARNING, "Please wait for the current scan to finish.").show();
            return;
        }
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
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}
