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
