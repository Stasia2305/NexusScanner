package com.nexusscan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scanned document, which consists of multiple pages.
 * Documents are identified by a barcode (or auto-generated split identifier) 
 * and track their progress through the scanning and QA workflow.
 */
public class Document {
    /**
     * Workflow status of the document.
     */
    public enum Status {
        IN_PROGRESS,    // Still being scanned
        WAITING_FOR_QA, // Scanned and awaiting review
        QA_COMPLETED    // Reviewed and exported
    }

    private int id;
    private String barcode;
    private Status status = Status.IN_PROGRESS;
    private List<Page> pages = new ArrayList<>();

    public Document(int id, String barcode) {
        this.id = id;
        this.barcode = barcode;
    }

    public int getId() { return id; }
    public String getBarcode() { return barcode; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<Page> getPages() { return pages; }
    public void addPage(Page page) { this.pages.add(page); }
}
