package com.nexusscan.controller;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.VBox;

/**
 * The AdminController manages the Administrative Dashboard.
 * It adheres to the Presentation Layer of the 3-layered architecture.
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

    private final UserService userService = new UserService();
    private final ProfileService profileService = new ProfileService();
    private final MetadataService metadataService = new MetadataService();
    private final LoggingService loggingService = LoggingService.getInstance();

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
            try {
                User user = new User(username, password, role);
                userService.registerUser(user);
                loggingService.log("Admin created user: " + username, AppState.getInstance().getCurrentUsernameSafe());
                refreshUserList();
                newUsernameField.clear();
                newPasswordField.clear();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to create user: " + e.getMessage()).show();
            }
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
            try {
                userService.deleteUser(selected);
                loggingService.log("Admin deleted user: " + selected, AppState.getInstance().getCurrentUsernameSafe());
                refreshUserList();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to delete user").show();
            }
        }
    }

    @FXML
    private void onAddMetadataFieldClick() {
        String name = newMetadataFieldName.getText();
        if (!name.isEmpty()) {
            try {
                metadataService.addField(name);
                refreshMetadataFieldList();
                newMetadataFieldName.clear();
                loggingService.log("Admin added metadata field: " + name, AppState.getInstance().getCurrentUsernameSafe());
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to add metadata field").show();
            }
        }
    }

    @FXML
    private void onRefreshLogsClick() {
        systemLogListView.setItems(FXCollections.observableArrayList(loggingService.getRecentLogs()));
    }

    private void refreshUserList() {
        try {
            List<String> usernames = userService.getAllUsers().stream().map(User::getUsername).toList();
            userListView.setItems(FXCollections.observableArrayList(usernames));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshProfileList() {
        try {
            List<String> profiles = profileService.getAllProfiles().stream().map(Profile::getName).toList();
            profileListView.setItems(FXCollections.observableArrayList(profiles));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshMetadataFieldList() {
        try {
            List<String> fields = metadataService.getAllFields().stream().map(f -> f.getFieldName()).toList();
            metadataFieldListView.setItems(FXCollections.observableArrayList(fields));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddProfileClick() {
        String name = profileNameField.getText();
        String logic = splitLogicField.getText();
        if (!name.isEmpty()) {
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
                try {
                    profileService.createProfile(new Profile(name, logic, settings));
                    loggingService.log("Admin created profile: " + name, AppState.getInstance().getCurrentUsernameSafe());
                    refreshProfileList();
                    profileNameField.clear();
                    splitLogicField.clear();
                    new Alert(Alert.AlertType.INFORMATION, "Profile created successfully").show();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Failed to create profile").show();
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

    @FXML
    private void onAssignProfileClick() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedProfileName = profileListView.getSelectionModel().getSelectedItem();
        
        if (selectedUser != null && selectedProfileName != null) {
            try {
                profileService.assignProfileToUser(selectedUser, selectedProfileName);
                loggingService.log("Admin assigned profile " + selectedProfileName + " to " + selectedUser, AppState.getInstance().getCurrentUsernameSafe());
                new Alert(Alert.AlertType.INFORMATION, "Profile assigned successfully").show();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to assign profile").show();
            }
        }
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
