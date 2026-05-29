package com.nexusscan.model;

/**
 * Represents a single scanned page within a document.
 * Stores image data (as byte array for BLOB storage) and display attributes like rotation.
 */
public class Page {
    private int id;
    private int pageNumber;
    private String imagePath; // Reference path for local preview
    private double rotation;   // Display rotation in degrees
    private byte[] imageData; // Raw image bytes for database storage

    public Page(int id, int pageNumber, String imagePath) {
        this.id = id;
        this.pageNumber = pageNumber;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public String getImagePath() { return imagePath; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}
