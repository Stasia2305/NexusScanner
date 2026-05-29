package com.nexusscan.controller;

import com.nexusscan.model.Profile;
import com.nexusscan.service.AppState;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;

/**
 * Controller for the regular User dashboard.
 * Allows users to select an assigned scanning profile and input a box identifier 
 * before starting a scanning session.
 */
public class UserController {
    @FXML private ComboBox<Profile> profileComboBox;
    @FXML private TextField boxIdField;

    @FXML
    public void initialize() {
        // Load only the profiles that the admin has assigned to this user
        String username = AppState.getInstance().getCurrentUser().getUsername();
        profileComboBox.setItems(FXCollections.observableArrayList(AppState.getInstance().getAccessibleProfiles(username)));
        
        // Use a converter to display the profile name in the dropdown
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

    /**
     * Initializes a scanning session by passing the selected profile and box ID 
     * to the ScanningController.
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
    }
}
