package com.nexusscan.controller;

import com.nexusscan.model.User;
import com.nexusscan.service.AppState;
import com.nexusscan.service.LoggingService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Login view.
 * Handles user authentication and redirects to the appropriate dashboard based on user role.
 */
public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    protected void onLoginButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        User userByUsername = AppState.getInstance().getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst().orElse(null);

        if (userByUsername == null) {
            errorLabel.setText("Invalid username");
        } else if (!userByUsername.getPassword().equals(password)) {
            errorLabel.setText("Incorrect password");
        } else {
            AppState.getInstance().setCurrentUser(userByUsername);
            LoggingService.getInstance().log("User logged in", username);
            navigateToDashboard(userByUsername);
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
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading dashboard");
        }
    }
}
