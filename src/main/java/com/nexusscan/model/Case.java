package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Case is a specific set of documents that are scanned together inside a box.
 * Each case has its own documents and special information (metadata).
 */
public class Case {
    private int id;
    private String caseNumber;
    private String metadata; // Extra information about the case, stored as text
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
