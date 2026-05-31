package com.nexusscan.controller;

import com.nexusscan.model.User;
import com.nexusscan.service.AppState;
import com.nexusscan.service.LoggingService;
import com.nexusscan.service.MetadataService;
import com.nexusscan.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The LoginController handles the login screen.
 * It checks the username and password and sends the user to the correct dashboard 
 * depending on whether they are an Admin or a regular User.
 */
public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();
    private final AppState appState = AppState.getInstance();

    @FXML
    protected void onLoginButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        try {
            User authenticatedUser = userService.authenticate(username, password);

            if (authenticatedUser == null) {
                errorLabel.setText("Invalid username or password");
            } else {
                appState.setCurrentUser(authenticatedUser);
                
                // Load metadata fields into AppState for scanning
                MetadataService metadataService = new MetadataService();
                appState.setMetadataFields(metadataService.getAllFields());
                
                LoggingService.getInstance().log("User logged in", username);
                navigateToDashboard(authenticatedUser);
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error during login");
        }
    }

    private void navigateToDashboard(User user) {
        String fxmlFile = user.getRole() == User.Role.ADMIN ? "admin-dashboard.fxml" : "user-dashboard.fxml";
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nexusscan/" + fxmlFile));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(user.getRole() == User.Role.ADMIN ? "Admin Dashboard" : "User Dashboard");
            stage.setResizable(true);
            stage.setMaximized(true);
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlFile);
            errorLabel.setText("Error loading dashboard");
        }
    }
}
