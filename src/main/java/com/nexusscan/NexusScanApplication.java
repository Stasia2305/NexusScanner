package com.nexusscan;

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
    public void start(Stage stage) throws IOException {
        // Initialize database structure
        try {
            com.nexusscan.service.DatabaseService.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
            // We can't show an alert here easily because the UI isn't ready yet,
            // but at least it will be in the logs.
        }

        // Load the login screen design from the FXML file
        FXMLLoader fxmlLoader = new FXMLLoader(NexusScanApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        
        // Configure the window properties
        stage.setTitle("NexusScan - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        
        // Show the window to the user
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
