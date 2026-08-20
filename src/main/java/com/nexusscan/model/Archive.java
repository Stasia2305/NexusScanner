package com.nexusscan.model;

/**
 * An Archive is a collection of boxes that belong to a specific client.
 * It helps organize data in this order: Client -> Archive -> Box.
 */
public class Archive {
    private int id;
    private String name;

    /**
     * Constructs a new Archive with the specified ID and name.
     *
     * @param id   The unique identifier for the archive.
     * @param name The name of the archive.
     */
    public Archive(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the archive ID.
     *
     * @return The unique identifier of this archive.
     */
    public int getId() { return id; }

    /**
     * Gets the archive name.
     *
     * @return The name of this archive.
     */
    public String getName() { return name; }
}
