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

    /**
     * Constructs a new Profile with the specified name and split logic, using default settings and empty description.
     *
     * @param name       The name of the profile.
     * @param splitLogic The split logic configuration.
     */
    public Profile(String name, String splitLogic) {
        this(name, splitLogic, null, "");
    }

    /**
     * Constructs a new Profile with the specified name, split logic, and settings, using empty description.
     *
     * @param name       The name of the profile.
     * @param splitLogic The split logic configuration.
     * @param settings   The profile's settings map.
     */
    public Profile(String name, String splitLogic, Map<String, String> settings) {
        this(name, splitLogic, settings, "");
    }

    /**
     * Constructs a new Profile with all specified details.
     *
     * @param name        The name of the profile.
     * @param splitLogic  The split logic configuration.
     * @param settings    The profile's settings map.
     * @param description The profile's description.
     */
    public Profile(String name, String splitLogic, Map<String, String> settings, String description) {
        this.name = name;
        this.splitLogic = splitLogic;
        this.settings = settings != null ? settings : new HashMap<>();
        this.description = description != null ? description : "";
    }

    /**
     * Gets the profile name.
     *
     * @return The profile name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the split logic configuration.
     *
     * @return The split logic string.
     */
    public String getSplitLogic() {
        return splitLogic;
    }

    /**
     * Gets the profile description.
     *
     * @return The profile description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the profile description.
     *
     * @param description The new description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the profile settings map.
     *
     * @return A map of settings.
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Sets the profile settings map.
     *
     * @param settings The new settings map.
     */
    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    /**
     * Gets a single setting value by key, returning a default value if not found.
     *
     * @param key          The setting key to look up.
     * @param defaultValue The value to return if key is not found.
     * @return The setting value or the default value.
     */
    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}
