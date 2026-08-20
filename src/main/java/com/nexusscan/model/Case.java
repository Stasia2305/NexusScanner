package com.nexusscan.model;

/**
 * A Case is a specific set of documents that are scanned together inside a box.
 * Each case has its own special information (metadata).
 */
public class Case {
    private int id;
    private String caseNumber;
    private String metadata; // Extra information about the case, stored as text

    /**
     * Constructs a new Case with the specified ID and case number.
     *
     * @param id         The unique identifier for the case.
     * @param caseNumber The case barcode or identification number.
     */
    public Case(int id, String caseNumber) {
        this.id = id;
        this.caseNumber = caseNumber;
    }

    /**
     * Gets the database ID of the case.
     *
     * @return The unique identifier of the case.
     */
    public int getId() { return id; }

    /**
     * Gets the case number of the case.
     *
     * @return The case number.
     */
    public String getCaseNumber() { return caseNumber; }

    /**
     * Gets the metadata stored for this case.
     *
     * @return The metadata string.
     */
    public String getMetadata() { return metadata; }

    /**
     * Sets the metadata for this case.
     *
     * @param metadata The new metadata string.
     */
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
