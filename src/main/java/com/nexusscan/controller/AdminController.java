package com.nexusscan.controller;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.service.AppState;
import com.nexusscan.service.DatabaseService;
import com.nexusscan.service.LoggingService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.VBox;

/**
 * The AdminController manages the Administrative Dashboard.
 * It allows administrators to create users, set up scanning profiles, 
 * manage metadata fields, and view system logs.
 */
public class AdminController {
    @FXML private ListView<String> userListView;
    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;

    @FXML private ListView<String> profileListView;
    @FXML private TextField profileNameField;
    @FXML private TextField splitLogicField;

    @FXML private ListView<String> metadataFieldListView;
    @FXML private TextField newMetadataFieldName;
    @FXML private ListView<String> systemLogListView;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        refreshUserList();
        refreshProfileList();
        refreshMetadataFieldList();
        onRefreshLogsClick();
    }

    @FXML
    private void onAddUserClick() {
        String username = newUsernameField.getText();
        String password = newPasswordField.getText();
        User.Role role = roleComboBox.getValue();
        if (!username.isEmpty() && !password.isEmpty() && role != null) {
            boolean exists = AppState.getInstance().getUsers().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
            if (exists) {
                new Alert(Alert.AlertType.ERROR, "User already exists").show();
                return;
            }
            AppState.getInstance().addUser(new User(username, password, role));
            LoggingService.getInstance().log("Admin created user: " + username, AppState.getInstance().getCurrentUsernameSafe());
            refreshUserList();
            newUsernameField.clear();
            newPasswordField.clear();
        }
    }

    @FXML
    private void onDeleteUserClick() {
        String selected = userListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.equals("admin")) {
                new Alert(Alert.AlertType.ERROR, "Cannot delete the default admin account.").show();
                return;
            }
            if (selected.equals(AppState.getInstance().getCurrentUsernameSafe())) {
                new Alert(Alert.AlertType.ERROR, "You cannot delete your own account while logged in.").show();
                return;
            }
            AppState.getInstance().deleteUser(selected);
            LoggingService.getInstance().log("Admin deleted user: " + selected, AppState.getInstance().getCurrentUsernameSafe());
            refreshUserList();
        }
    }

    @FXML
    private void onAddMetadataFieldClick() {
        String name = newMetadataFieldName.getText();
        if (!name.isEmpty()) {
            AppState.getInstance().addMetadataField(name);
            refreshMetadataFieldList();
            newMetadataFieldName.clear();
            LoggingService.getInstance().log("Admin added metadata field: " + name, AppState.getInstance().getCurrentUsernameSafe());
        }
    }

    @FXML
    private void onDeleteMetadataFieldClick() {
        String selected = metadataFieldListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AppState.getInstance().removeMetadataField(selected);
            refreshMetadataFieldList();
            LoggingService.getInstance().log("Admin deleted metadata field: " + selected, AppState.getInstance().getCurrentUsernameSafe());
        }
    }

    @FXML
    private void onRefreshLogsClick() {
        List<String> logStrings = new ArrayList<>();
        try {
            DatabaseService db = DatabaseService.getInstance();
            try (Statement stmt = db.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM logs ORDER BY id DESC LIMIT 100")) {
                while (rs.next()) {
                    logStrings.add("[" + rs.getString("timestamp") + "] " + rs.getString("username") + ": " + rs.getString("action"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        systemLogListView.setItems(FXCollections.observableArrayList(logStrings));
    }

    private void refreshMetadataFieldList() {
        List<String> fields = AppState.getInstance().getMetadataFields().stream()
                .map(com.nexusscan.model.MetadataField::getFieldName)
                .toList();
        metadataFieldListView.setItems(FXCollections.observableArrayList(fields));
    }

    /**
     * Creates a new scanning profile (a set of rules for scanning).
     * It also opens a dialog to define extra settings like default rotation.
     */
    @FXML
    private void onAddProfileClick() {
        String name = profileNameField.getText();
        String logic = splitLogicField.getText();
        if (!name.isEmpty()) {
            if (logic != null && !logic.isEmpty()) {
                try {
                    int interval = Integer.parseInt(logic.trim());
                    if (interval <= 0) {
                        new Alert(Alert.AlertType.ERROR, "Split logic must be a positive integer.").show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    // It's allowed to be non-numeric (e.g. barcode pattern), 
                    // but for now we expect numeric or empty based on implementation.
                    // If we want to strictly enforce numeric:
                    new Alert(Alert.AlertType.ERROR, "Split logic must be a numeric interval.").show();
                    return;
                }
            }
            
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Profile Settings");
            dialog.setHeaderText("Define general settings for this profile (e.g., rotation=5)");
            
            ButtonType okButtonType = new ButtonType("Create Profile", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
            
            VBox content = new VBox(10);
            TextField settingsField = new TextField();
            settingsField.setPromptText("key1=value1;key2=value2");
            content.getChildren().addAll(new Label("Settings (semicolon separated):"), settingsField);
            dialog.getDialogPane().setContent(content);
            
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    return parseSettings(settingsField.getText());
                }
                return null;
            });
            
            dialog.showAndWait().ifPresent(settings -> {
                boolean success = AppState.getInstance().addProfile(new Profile(name, logic, settings));
                if (success) {
                    LoggingService.getInstance().log("Admin created profile: " + name + " with settings: " + settings, AppState.getInstance().getCurrentUsernameSafe());
                    refreshProfileList();
                    profileNameField.clear();
                    splitLogicField.clear();
                    new Alert(Alert.AlertType.INFORMATION, "Profile created successfully").show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to create profile. Name may already exist.").show();
                }
            });
        }
    }

    private Map<String, String> parseSettings(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        String[] pairs = s.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    /**
     * Connects a scanning profile to a specific user.
     * Users can only see and use profiles that have been assigned to them by an admin.
     */
    @FXML
    private void onAssignProfileClick() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedProfileName = profileListView.getSelectionModel().getSelectedItem();
        
        if (selectedUser != null && selectedProfileName != null) {
            Profile profile = AppState.getInstance().getAllProfiles().stream()
                    .filter(p -> p.getName().equals(selectedProfileName))
                    .findFirst().orElse(null);
            
            if (profile != null) {
                // P2 Fix: Duplicate assignment check
                boolean alreadyAssigned = AppState.getInstance().getAccessibleProfiles(selectedUser).stream()
                        .anyMatch(p -> p.getName().equals(selectedProfileName));
                if (alreadyAssigned) {
                    new Alert(Alert.AlertType.WARNING, "Profile already assigned to this user").show();
                    return;
                }
                AppState.getInstance().assignProfileToUser(selectedUser, profile);
                LoggingService.getInstance().log("Admin assigned profile " + selectedProfileName + " to " + selectedUser, AppState.getInstance().getCurrentUsernameSafe());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Profile assigned successfully");
                alert.show();
            }
        }
    }

    private void refreshUserList() {
        userListView.setItems(FXCollections.observableArrayList(
                AppState.getInstance().getUsers().stream().map(User::getUsername).toList()
        ));
    }

    private void refreshProfileList() {
        profileListView.setItems(FXCollections.observableArrayList(
                AppState.getInstance().getAllProfiles().stream().map(Profile::getName).toList()
        ));
    }

    @FXML
    private void onLogoutClick() throws IOException {
        AppState.getInstance().setCurrentUser(null);
        navigateToLogin();
    }

    private void navigateToLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = (Stage) userListView.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}
