package com.nexusscan.model;

/**
 * A Client is the top-level entity, such as a company or organization.
 * Each client can have multiple archives.
 */
public class Client {
    private int id;
    private String name;

    /**
     * Constructs a new Client with the specified ID and name.
     *
     * @param id   The unique identifier for the client.
     * @param name The name of the client.
     */
    public Client(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the database ID of the client.
     *
     * @return The unique client ID.
     */
    public int getId() { return id; }

    /**
     * Gets the name of the client.
     *
     * @return The client name.
     */
    public String getName() { return name; }
}
