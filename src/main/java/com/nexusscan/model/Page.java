package com.nexusscan.model;

/**
 * A Page represents a single scanned sheet of paper within a document.
 * It stores the image file path, how much it should be rotated, and the actual image data.
 */
public class Page {
    private int id;
    private int pageNumber;
    private String imagePath; // Where the image is stored on the computer
    private double rotation;   // How many degrees the image should be turned
    private byte[] imageData; // The actual image data saved in the database

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
