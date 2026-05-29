package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical container (Box) holding multiple scanning cases.
 * Part of the organization hierarchy: Client -> Archive -> Box -> Case.
 */
public class Box {
    private int id;
    private String boxId;
    private List<Case> cases = new ArrayList<>();

    public Box(int id, String boxId) {
        this.id = id;
        this.boxId = boxId;
    }

    public int getId() { return id; }
    public String getBoxId() { return boxId; }
    public List<Case> getCases() { return cases; }
}
