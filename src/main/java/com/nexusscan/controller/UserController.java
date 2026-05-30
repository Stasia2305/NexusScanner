package com.nexusscan.controller;

import com.nexusscan.model.Profile;
import com.nexusscan.model.User;
import com.nexusscan.service.AppState;
import com.nexusscan.service.ProfileService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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

    @FXML
    public void initialize() {
        try {
            // Load only the profiles that the admin has assigned to this user
            User user = AppState.getInstance().getCurrentUser();
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
                e.printStackTrace();
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
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Missing Information");
            alert.setHeaderText(null);
            alert.setContentText("Please select a scanning profile and enter a Box ID.");
            alert.showAndWait();
        }
    }

    @FXML
    private void onLogoutClick() throws IOException {
        AppState.getInstance().setCurrentUser(null);
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
