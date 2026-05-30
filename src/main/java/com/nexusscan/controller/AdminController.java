package com.nexusscan.controller;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.service.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AdminController manages the Administrative Dashboard.
 * It adheres to the Presentation Layer of the 3-layered architecture.
 */
public class AdminController {
    // User Management
    @FXML private ListView<String> userListView;
    @FXML private TextField userSearchField;
    @FXML private VBox userDetailsBox;
    @FXML private StackPane userPlaceholder;
    @FXML private Label selectedUserLabel;
    @FXML private ListView<String> assignedProfilesListView;
    @FXML private ComboBox<Profile> quickProfileComboBox;

    // Profile Management
    @FXML private ListView<String> profileListView;
    @FXML private TextField profileSearchField;
    @FXML private TextField profileNameField;
    @FXML private TextField splitLogicField;

    // Registration
    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;

    // Metadata
    @FXML private ListView<String> metadataFieldListView;
    @FXML private TextField metadataSearchField;
    @FXML private TextField newMetadataFieldName;

    // Logs
    @FXML private ListView<String> systemLogListView;
    @FXML private TextField logSearchField;

    private final UserService userService = new UserService();
    private final ProfileService profileService = new ProfileService();
    private final MetadataService metadataService = new MetadataService();
    private final LoggingService loggingService = LoggingService.getInstance();

    private final ObservableList<String> masterUserList = FXCollections.observableArrayList();
    private final FilteredList<String> filteredUserList = new FilteredList<>(masterUserList);

    private final ObservableList<String> masterProfileList = FXCollections.observableArrayList();
    private final FilteredList<String> filteredProfileList = new FilteredList<>(masterProfileList);

    private final ObservableList<Profile> allProfilesObjects = FXCollections.observableArrayList();

    private final ObservableList<String> masterLogList = FXCollections.observableArrayList();
    private final FilteredList<String> filteredLogList = new FilteredList<>(masterLogList);

    private final ObservableList<String> masterMetadataList = FXCollections.observableArrayList();
    private final FilteredList<String> filteredMetadataList = new FilteredList<>(masterMetadataList);

    @FXML
    public void initialize() {
        setupListsAndFiltering();
        setupSelectionListeners();
        setupConverters();
        
        refreshAllData();
    }

    private void setupListsAndFiltering() {
        userListView.setItems(filteredUserList);
        profileListView.setItems(filteredProfileList);
        systemLogListView.setItems(filteredLogList);
        metadataFieldListView.setItems(filteredMetadataList);
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        quickProfileComboBox.setItems(allProfilesObjects);

        userSearchField.textProperty().addListener((obs, old, val) -> 
            filteredUserList.setPredicate(u -> val == null || val.isEmpty() || u.toLowerCase().contains(val.toLowerCase())));
        
        profileSearchField.textProperty().addListener((obs, old, val) -> 
            filteredProfileList.setPredicate(p -> val == null || val.isEmpty() || p.toLowerCase().contains(val.toLowerCase())));

        logSearchField.textProperty().addListener((obs, old, val) -> 
            filteredLogList.setPredicate(l -> val == null || val.isEmpty() || l.toLowerCase().contains(val.toLowerCase())));

        metadataSearchField.textProperty().addListener((obs, old, val) -> 
            filteredMetadataList.setPredicate(m -> val == null || val.isEmpty() || m.toLowerCase().contains(val.toLowerCase())));
    }

    private void setupSelectionListeners() {
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showUserDetails(newVal);
            } else {
                hideUserDetails();
            }
        });
    }

    private void setupConverters() {
        quickProfileComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Profile p) { return p == null ? "" : p.getName(); }
            @Override public Profile fromString(String s) { return null; }
        });
    }

    private void showUserDetails(String username) {
        selectedUserLabel.setText(username);
        userDetailsBox.setDisable(false);
        userDetailsBox.setVisible(true);
        userPlaceholder.setVisible(false);
        refreshAssignedProfiles(username);
    }

    private void hideUserDetails() {
        userDetailsBox.setDisable(true);
        userDetailsBox.setVisible(false);
        userPlaceholder.setVisible(true);
        assignedProfilesListView.getItems().clear();
    }

    private void refreshAssignedProfiles(String username) {
        try {
            List<String> assigned = profileService.getAccessibleProfiles(username).stream().map(Profile::getName).toList();
            assignedProfilesListView.setItems(FXCollections.observableArrayList(assigned));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onQuickAssignClick() {
        String user = userListView.getSelectionModel().getSelectedItem();
        Profile profile = quickProfileComboBox.getValue();
        if (user != null && profile != null) {
            try {
                profileService.assignProfileToUser(user, profile.getName());
                loggingService.log("Assigned profile " + profile.getName() + " to " + user, AppState.getInstance().getCurrentUsernameSafe());
                refreshAssignedProfiles(user);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Assignment failed: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void onRemoveAssignmentClick() {
        String user = userListView.getSelectionModel().getSelectedItem();
        String profile = assignedProfilesListView.getSelectionModel().getSelectedItem();
        if (user != null && profile != null) {
            try {
                profileService.removeProfileFromUser(user, profile);
                loggingService.log("Removed profile " + profile + " from " + user, AppState.getInstance().getCurrentUsernameSafe());
                refreshAssignedProfiles(user);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Removal failed: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void onAddUserClick() {
        String username = newUsernameField.getText();
        String password = newPasswordField.getText();
        User.Role role = roleComboBox.getValue();
        if (!username.isEmpty() && !password.isEmpty() && role != null) {
            try {
                userService.registerUser(new User(username, password, role));
                loggingService.log("Created user: " + username, AppState.getInstance().getCurrentUsernameSafe());
                refreshUserList();
                newUsernameField.clear();
                newPasswordField.clear();
                new Alert(Alert.AlertType.INFORMATION, "User registered successfully").show();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Registration failed: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void onDeleteUserClick() {
        String selected = userListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.equals("admin")) {
                new Alert(Alert.AlertType.ERROR, "Cannot delete 'admin'").show();
                return;
            }
            try {
                userService.deleteUser(selected);
                loggingService.log("Deleted user: " + selected, AppState.getInstance().getCurrentUsernameSafe());
                refreshUserList();
                hideUserDetails();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Deletion failed").show();
            }
        }
    }

    @FXML
    private void onAddProfileClick() {
        String name = profileNameField.getText();
        String logic = splitLogicField.getText();
        if (!name.isEmpty()) {
            try {
                profileService.createProfile(new Profile(name, logic));
                loggingService.log("Created profile: " + name, AppState.getInstance().getCurrentUsernameSafe());
                refreshProfileList();
                profileNameField.clear();
                splitLogicField.clear();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Profile creation failed").show();
            }
        }
    }

    @FXML
    private void onDeleteProfileClick() {
        String selected = profileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                profileService.deleteProfile(selected);
                loggingService.log("Deleted profile: " + selected, AppState.getInstance().getCurrentUsernameSafe());
                refreshProfileList();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Profile deletion failed").show();
            }
        }
    }

    @FXML
    private void onAddMetadataFieldClick() {
        String name = newMetadataFieldName.getText();
        if (!name.isEmpty()) {
            try {
                metadataService.addField(name);
                loggingService.log("Added metadata field: " + name, AppState.getInstance().getCurrentUsernameSafe());
                refreshMetadataFieldList();
                newMetadataFieldName.clear();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Field addition failed").show();
            }
        }
    }

    @FXML
    private void onDeleteMetadataFieldClick() {
        String selected = metadataFieldListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                metadataService.deleteField(selected);
                loggingService.log("Deleted metadata field: " + selected, AppState.getInstance().getCurrentUsernameSafe());
                refreshMetadataFieldList();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Field deletion failed").show();
            }
        }
    }

    @FXML
    private void onRefreshLogsClick() {
        masterLogList.setAll(loggingService.getRecentLogs());
    }

    private void refreshAllData() {
        refreshUserList();
        refreshProfileList();
        refreshMetadataFieldList();
        onRefreshLogsClick();
    }

    private void refreshUserList() {
        try {
            masterUserList.setAll(userService.getAllUsers().stream().map(User::getUsername).toList());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshProfileList() {
        try {
            List<Profile> profiles = profileService.getAllProfiles();
            allProfilesObjects.setAll(profiles);
            masterProfileList.setAll(profiles.stream().map(Profile::getName).toList());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshMetadataFieldList() {
        try {
            masterMetadataList.setAll(metadataService.getAllFields().stream().map(f -> f.getFieldName()).toList());
        } catch (SQLException e) { e.printStackTrace(); }
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
