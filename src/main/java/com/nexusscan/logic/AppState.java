package com.nexusscan.logic;

import com.nexusscan.model.User;
import com.nexusscan.model.MetadataField;

import java.util.ArrayList;
import java.util.List;

/**
 * The AppState holds the current runtime state of the application.
 * It follows the Singleton pattern and is part of the Logic Layer.
 */
public class AppState {
    private static AppState instance;
    private User currentUser;
    private List<MetadataField> metadataFields = new ArrayList<>();

    /**
     * Private constructor for the Singleton pattern.
     */
    private AppState() {}

    /**
     * Provides the global instance of the AppState.
     *
     * @return The Singleton AppState instance.
     */
    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    /**
     * Gets the currently logged-in user.
     *
     * @return The current User object, or null if no user is logged in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the currently logged-in user.
     *
     * @param currentUser The User object to set.
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Gets the username of the current user, or a safe default ("SYSTEM") if no user is logged in.
     *
     * @return A non-null username string.
     */
    public String getCurrentUsernameSafe() {
        return (currentUser != null) ? currentUser.getUsername() : "SYSTEM";
    }

    /**
     * Gets the list of available metadata fields.
     *
     * @return A list of MetadataField objects.
     */
    public List<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    /**
     * Sets the list of available metadata fields.
     *
     * @param metadataFields The list of MetadataField objects to set.
     */
    public void setMetadataFields(List<MetadataField> metadataFields) {
        this.metadataFields = metadataFields;
    }
}
