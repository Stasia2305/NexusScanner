package com.nexusscan.model;

/**
 * A Case is a specific set of documents that are scanned together inside a box.
 * Each case has its own special information (metadata).
 */
public class Case {
    private int id;
    private String caseNumber;
    private String metadata; // Extra information about the case, stored as text

    public Case(int id, String caseNumber) {
        this.id = id;
        this.caseNumber = caseNumber;
    }

    public int getId() { return id; }
    public String getCaseNumber() { return caseNumber; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
