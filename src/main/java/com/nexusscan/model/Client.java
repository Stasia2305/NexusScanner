package com.nexusscan.model;

/**
 * A Client is the top-level entity, such as a company or organization.
 * Each client can have multiple archives.
 */
public class Client {
    private int id;
    private String name;

    public Client(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
