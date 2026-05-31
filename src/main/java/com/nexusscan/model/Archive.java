package com.nexusscan.model;

/**
 * An Archive is a collection of boxes that belong to a specific client.
 * It helps organize data in this order: Client -> Archive -> Box.
 */
public class Archive {
    private int id;
    private String name;

    public Archive(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
