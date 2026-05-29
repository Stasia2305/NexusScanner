package com.nexusscan.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines a scanning configuration (Profile).
 * Profiles specify how documents should be split (e.g., every N pages) 
 * and define default processing settings like rotation.
 */
public class Profile {
    private String name;
    private String splitLogic; // Numeric interval for auto-splitting documents
    private Map<String, String> settings = new HashMap<>(); // General key-value settings (e.g., rotation=5)

    public Profile(String name, String splitLogic) {
        this.name = name;
        this.splitLogic = splitLogic;
    }

    public Profile(String name, String splitLogic, Map<String, String> settings) {
        this.name = name;
        this.splitLogic = splitLogic;
        this.settings = settings != null ? settings : new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getSplitLogic() {
        return splitLogic;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}
