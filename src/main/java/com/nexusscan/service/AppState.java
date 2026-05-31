package com.nexusscan.service;

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

    private AppState() {}

    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getCurrentUsernameSafe() {
        return (currentUser != null) ? currentUser.getUsername() : "SYSTEM";
    }

    public List<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<MetadataField> metadataFields) {
        this.metadataFields = metadataFields;
    }
}
