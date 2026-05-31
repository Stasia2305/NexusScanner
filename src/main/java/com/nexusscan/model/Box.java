package com.nexusscan.model;

/**
 * A Box is a physical container that holds multiple scanning cases.
 * It is part of the organization: Client -> Archive -> Box -> Case.
 */
public class Box {
    private int id;
    private String boxId;

    public Box(int id, String boxId) {
        this.id = id;
        this.boxId = boxId;
    }

    public int getId() { return id; }
    public String getBoxId() { return boxId; }
}
