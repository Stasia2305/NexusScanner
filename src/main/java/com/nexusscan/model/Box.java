package com.nexusscan.model;

/**
 * A Box is a physical container that holds multiple scanning cases.
 * It is part of the organization: Client -> Archive -> Box -> Case.
 */
public class Box {
    private int id;
    private String boxId;

    /**
     * Constructs a new Box with the specified ID and physical barcode ID.
     *
     * @param id    The unique identifier for the box in the database.
     * @param boxId The physical barcode identifier of the box.
     */
    public Box(int id, String boxId) {
        this.id = id;
        this.boxId = boxId;
    }

    /**
     * Gets the database ID of the box.
     *
     * @return The unique database identifier of this box.
     */
    public int getId() { return id; }

    /**
     * Gets the physical barcode ID of the box.
     *
     * @return The barcode identifier of this box.
     */
    public String getBoxId() { return boxId; }
}
