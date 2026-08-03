package com.nexusscan.presentation;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.logic.AppState;
import com.nexusscan.logic.ProfileService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The UserController manages the dashboard for regular users.
 * It allows users to choose a scanning profile and enter a Box ID before they start scanning.
 */
public class UserController {
    @FXML private ComboBox<Profile> profileComboBox;
    @FXML private TextField boxIdField;
    @FXML private Label connectionLabel;

    private final ProfileService profileService = new ProfileService();
    private final AppState appState = AppState.getInstance();

    @FXML
    public void initialize() {
        try {
            // Load only the profiles that the admin has assigned to this user
            User user = appState.getCurrentUser();
            if (user == null) {
                System.err.println("Error: No current user set in AppState");
                return;
            }
            
            String username = user.getUsername();
            if (connectionLabel != null) {
                connectionLabel.setText("Connected as: " + username);
            }
            try {
                if (profileComboBox != null) {
                    profileComboBox.setItems(FXCollections.observableArrayList(profileService.getAccessibleProfiles(username)));
                }
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to load profiles: " + e.getMessage()).showAndWait();
            }
            
            // Use a converter to display the profile name in the dropdown
            if (profileComboBox != null) {
                profileComboBox.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Profile profile) {
                        return profile == null ? "" : profile.getName();
                    }

                    @Override
                    public Profile fromString(String s) {
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Critical error during UserController initialization:");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Opens the scanning workspace using the selected profile and Box ID.
     */
    @FXML
    private void onStartScanningClick() throws IOException {
        Profile profile = profileComboBox.getValue();
        String boxId = boxIdField.getText();
        
        if (profile != null && !boxId.isEmpty()) {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/scanning-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            
            ScanningController controller = fxmlLoader.getController();
            controller.setSession(profile, boxId);
            
            Stage stage = (Stage) boxIdField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Scanning - " + profile.getName() + " - " + boxId);
            stage.setMaximized(true);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Information");
            alert.setHeaderText(null);
            alert.setContentText("Please select a scanning profile and enter a Box ID.");
            alert.showAndWait();
        }
    }

    @FXML
    private void onLogoutClick() throws IOException {
        appState.setCurrentUser(null);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = (Stage) boxIdField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}
