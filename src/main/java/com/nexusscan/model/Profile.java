package com.nexusscan.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A Profile is a set of rules for scanning documents.
 * It tells the system how to split documents (e.g., after every 5 pages) 
 * and what settings to use, like default rotation.
 */
public class Profile {
    private String name;
    private String splitLogic; // A rule for automatically splitting documents by page count
    private String description;
    private Map<String, String> settings = new HashMap<>(); // Extra settings like default rotation

    public Profile(String name, String splitLogic) {
        this(name, splitLogic, null, "");
    }

    public Profile(String name, String splitLogic, Map<String, String> settings) {
        this(name, splitLogic, settings, "");
    }

    public Profile(String name, String splitLogic, Map<String, String> settings, String description) {
        this.name = name;
        this.splitLogic = splitLogic;
        this.settings = settings != null ? settings : new HashMap<>();
        this.description = description != null ? description : "";
    }

    public String getName() {
        return name;
    }

    public String getSplitLogic() {
        return splitLogic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
