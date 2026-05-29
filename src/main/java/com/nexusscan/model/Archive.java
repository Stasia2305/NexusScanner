package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a logical grouping of physical boxes belonging to a client.
 * Part of the organization hierarchy: Client -> Archive -> Box.
 */
public class Archive {
    private int id;
    private String name;
    private List<Box> boxes = new ArrayList<>();

    public Archive(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Box> getBoxes() { return boxes; }
}
