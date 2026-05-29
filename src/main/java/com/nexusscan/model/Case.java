package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specific scanning case within a physical box.
 * Stores a collection of scanned documents and associated case-level metadata.
 */
public class Case {
    private int id;
    private String caseNumber;
    private String metadata; // Serialized metadata values
    private List<Document> documents = new ArrayList<>();

    public Case(int id, String caseNumber) {
        this.id = id;
        this.caseNumber = caseNumber;
    }

    public int getId() { return id; }
    public String getCaseNumber() { return caseNumber; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public List<Document> getDocuments() { return documents; }
}
