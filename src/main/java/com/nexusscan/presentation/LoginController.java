package com.nexusscan.presentation;

import com.nexusscan.model.User;
import com.nexusscan.logic.AppState;
import com.nexusscan.logic.LoggingService;
import com.nexusscan.logic.MetadataService;
import com.nexusscan.logic.UserService;
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
 * The LoginController handles the authentication screen.
 * It validates credentials against the database or provides access via 
 * system fallbacks (admin/admin, user/user) in offline mode.
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
