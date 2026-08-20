package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Document is a collection of scanned pages.
 * Each document is identified by a barcode and has a status to track its progress.
 */
public class Document {
    /**
     * The current stage of the document in the scanning process.
     */
    public enum Status {
        IN_PROGRESS,    // Still being scanned
        WAITING_FOR_QA, // Scanned and waiting for review
        QA_COMPLETED    // Reviewed and ready to be exported
    }

    private int id;
    private String barcode;
    private Status status = Status.IN_PROGRESS;
    private List<Page> pages = new ArrayList<>();

    /**
     * Constructs a new Document with the specified ID and barcode.
     *
     * @param id      The unique identifier for the document.
     * @param barcode The barcode associated with this document.
     */
    public Document(int id, String barcode) {
        this.id = id;
        this.barcode = barcode;
    }

    /**
     * Gets the database ID of the document.
     *
     * @return The unique identifier of the document.
     */
    public int getId() { return id; }

    /**
     * Gets the barcode of the document.
     *
     * @return The barcode string.
     */
    public String getBarcode() { return barcode; }

    /**
     * Gets the current status of the document.
     *
     * @return The status enum value.
     */
    public Status getStatus() { return status; }

    /**
     * Sets the status of the document.
     *
     * @param status The new status to set.
     */
    public void setStatus(Status status) { this.status = status; }

    /**
     * Gets the list of pages in the document.
     *
     * @return A list of Page objects.
     */
    public List<Page> getPages() { return pages; }

    /**
     * Adds a scanned page to this document.
     *
     * @param page The Page object to add.
     */
    public void addPage(Page page) { this.pages.add(page); }
}
