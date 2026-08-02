package com.nexusscan;

import com.nexusscan.dal.DatabaseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The NexusScanApplication class sets up the main window and user interface.
 * It is responsible for loading the login screen when the app starts.
 */
public class NexusScanApplication extends Application {
    /**
     * This method is called automatically when the application starts.
     * It sets up the stage (the window) and the scene (the content).
     */
    @Override
    public void start(Stage stage) {
        // Initialize database structure - non-fatal
        try {
            DatabaseService.getInstance();
        } catch (Exception e) {
            System.err.println("Database initialization failed (continuing to UI): " + e.getMessage());
        }

        // Load the login screen design from the FXML file
        try {
            var resource = getClass().getResource("/com/nexusscan/login-view.fxml");
            if (resource == null) {
                throw new IOException("Could not find login-view.fxml at /com/nexusscan/login-view.fxml");
            }
            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            Scene scene = new Scene(fxmlLoader.load());

            // Configure the window properties
            stage.setTitle("NexusScan - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();

            // Show the window to the user
            stage.show();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to load application UI.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
