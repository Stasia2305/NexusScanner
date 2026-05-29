package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the top-level entity in the organizational hierarchy.
 * A client owns multiple archives.
 */
public class Client {
    private int id;
    private String name;
    private List<Archive> archives = new ArrayList<>();

    public Client(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Archive> getArchives() { return archives; }
}
