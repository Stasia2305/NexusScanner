package com.nexusscan.presentation;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.logic.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.nexusscan.model.MetadataField;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The AdminController manages the Administrative Dashboard.
 * It provides CRUD operations for users and metadata fields, and displays system logs.
 * Adheres to the Presentation Layer of the 3-layered architecture.
 */
public class AdminController {
    private static final String PROTECTED_ADMIN_USERNAME = "admin";
    // User Management
    @FXML private ListView<String> userListView;
    @FXML private TextField userSearchField;
    @FXML private HBox userDetailsBox;
    @FXML private Label selectedUserLabel;
    @FXML private ListView<String> assignedProfilesListView;
    @FXML private ComboBox<Profile> quickProfileComboBox;

    // Profile Management
    @FXML private ListView<String> profileListView;
    @FXML private TextField profileSearchField;
    @FXML private TextField profileNameField;
    @FXML private TextArea profileDescriptionArea;
    @FXML private TextField splitLogicField;

    // Registration
    @FXML private TextField newUsernameField;
    @FXML private TextField newEmailField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;

    // Metadata
    @FXML private ListView<String> metadataFieldListView;
    @FXML private TextField newMetadataFieldName;

    // Logs
    @FXML private ListView<String> systemLogListView;
    @FXML private TextField logSearchField;

    private final UserService userService = new UserService();
    private final ProfileService profileService = new ProfileService();
    private final MetadataService metadataService = new MetadataService();
    private final LoggingService loggingService = LoggingService.getInstance();
    private final AppState appState = AppState.getInstance();

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
        try {
            User user = appState.getCurrentUser();
            if (user == null) {
                System.err.println("Error: No current user set in AppState for Admin");
            }
            
            setupListsAndFiltering();
            setupSelectionListeners();
            setupConverters();
            
            refreshAllData();
        } catch (Exception e) {
            System.err.println("Critical error during AdminController initialization:");
            e.printStackTrace();
            throw e; // Rethrow to let FXMLLoader know it failed
        }
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
        refreshAssignedProfiles(username);
    }

    private void hideUserDetails() {
        userDetailsBox.setDisable(true);
        userDetailsBox.setVisible(false);
        assignedProfilesListView.getItems().clear();
    }

    private void refreshAssignedProfiles(String username) {
        try {
            List<String> assigned = profileService.getAccessibleProfiles(username).stream().map(Profile::getName).toList();
            assignedProfilesListView.setItems(FXCollections.observableArrayList(assigned));
        } catch (SQLException e) {
            //e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load profiles:" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onQuickAssignClick() {
        String user = userListView.getSelectionModel().getSelectedItem();
        Profile profile = quickProfileComboBox.getValue();
        if (user != null && profile != null) {
            try {
                profileService.assignProfileToUser(user, profile.getName());
                loggingService.log("Assigned profile " + profile.getName() + " to " + user, appState.getCurrentUsernameSafe());
                refreshAssignedProfiles(user);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Assignment failed: " + e.getMessage()).showAndWait();
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
                loggingService.log("Removed profile " + profile + " from " + user, appState.getCurrentUsernameSafe());
                refreshAssignedProfiles(user);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Removal failed: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onAddUserClick() {
        String username = newUsernameField.getText();
        String email = newEmailField.getText();
        String password = newPasswordField.getText();
        User.Role role = roleComboBox.getValue();
        if (email.isEmpty() || !email.contains("@")) {
            new Alert(Alert.AlertType.WARNING, "Please enter a valid email address").showAndWait();
            return;
        }
        if (!username.isEmpty() && !password.isEmpty() && role != null) {
            try {
                userService.registerUser(new User(username, password, email, role));
                loggingService.log("Created user: " + username, appState.getCurrentUsernameSafe());
                refreshUserList();
                newUsernameField.clear();
                newEmailField.clear();
                newPasswordField.clear();
                new Alert(Alert.AlertType.INFORMATION, "User registered successfully").showAndWait();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Registration failed: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onDeleteUserClick() {
        String selected = userListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.equals(PROTECTED_ADMIN_USERNAME)) {
                new Alert(Alert.AlertType.ERROR, "Cannot delete 'admin'").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Delete user '" + selected + "'? This cannot be undone.");
                if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isEmpty()) {
                    return; // User cancelled
                }
                try {
                userService.deleteUser(selected);
                loggingService.log("Deleted user: " + selected, appState.getCurrentUsernameSafe());
                refreshUserList();
                hideUserDetails();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Deletion failed").showAndWait();
            }
        }
    }

    @FXML
    private void onAddProfileClick() {
        String name = profileNameField.getText();
        String logic = splitLogicField.getText();
        String description = (profileDescriptionArea != null) ? profileDescriptionArea.getText() : "";
        if (!name.isEmpty()) {
            try {
                Profile profile = new Profile(name, logic, null, description);
                profileService.createProfile(profile);
                loggingService.log("Created profile: " + name, appState.getCurrentUsernameSafe());
                refreshProfileList();
                profileNameField.clear();
                splitLogicField.clear();
                if (profileDescriptionArea != null) profileDescriptionArea.clear();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Profile creation failed").showAndWait();
            }
        }
    }

    @FXML
    private void onDeleteProfileClick() {
        String selected = profileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Delete profile '" + selected + "'? This cannot be undone and will remove access for all users assigned to it.");
                if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isEmpty()) {
                    return; // User cancelled
                }
            try {
                profileService.deleteProfile(selected);
                loggingService.log("Deleted profile: " + selected, appState.getCurrentUsernameSafe());
                refreshProfileList();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Profile deletion failed").showAndWait();
            }
        }
    }

    @FXML
    private void onAddMetadataFieldClick() {
        String name = newMetadataFieldName.getText();
        if (!name.isEmpty()) {
            try {
                metadataService.addField(name);
                loggingService.log("Added metadata field: " + name, appState.getCurrentUsernameSafe());
                refreshMetadataFieldList();
                newMetadataFieldName.clear();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Field addition failed").showAndWait();
            }
        }
    }

    @FXML
    private void onDeleteMetadataFieldClick() {
        String selected = metadataFieldListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                metadataService.deleteField(selected);
                loggingService.log("Deleted metadata field: " + selected, appState.getCurrentUsernameSafe());
                refreshMetadataFieldList();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Field deletion failed").showAndWait();
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
        } catch (SQLException e) { 
            new Alert(Alert.AlertType.ERROR, "Failed to load users:" + e.getMessage()).showAndWait();}
    }

    private void refreshProfileList() {
        try {
            List<Profile> profiles = profileService.getAllProfiles();
            allProfilesObjects.setAll(profiles);
            masterProfileList.setAll(profiles.stream().map(Profile::getName).toList());
        } catch (SQLException e) { 
            new Alert(Alert.AlertType.ERROR, "Failed to load profiles:" + e.getMessage()).showAndWait();
         }
    }

    private void refreshMetadataFieldList() {
        try {
            masterMetadataList.setAll(metadataService.getAllFields().stream().map(MetadataField::getFieldName).toList());
        } catch (SQLException e) { 
            new Alert(Alert.AlertType.ERROR, "Failed to load metadata fields:" + e.getMessage()).showAndWait();
         }
    }

    @FXML
    private void onLogoutClick() throws IOException {
        appState.setCurrentUser(null);
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
