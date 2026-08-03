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
        // Initialize the database connection. 
        // A short timeout is used to ensure the app starts quickly even if the server is offline.
        try {
            DatabaseService.getInstance();
        } catch (Exception e) {
            System.err.println("Database initialization failed (continuing in Offline Mode): " + e.getMessage());
        }

        // Load the initial login screen from the FXML resource
        try {
            var resource = getClass().getResource("/com/nexusscan/login-view.fxml");
            if (resource == null) {
                throw new IOException("Could not find login-view.fxml at /com/nexusscan/login-view.fxml");
            }
            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            Scene scene = new Scene(fxmlLoader.load());

            // Configure the main window (Stage)
            stage.setTitle("NexusScan - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();

            // Display the UI to the user
            stage.show();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to load application UI. The FXML file may be missing or corrupted.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
