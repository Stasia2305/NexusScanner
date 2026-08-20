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

    /**
     * Constructs a new Page with the specified ID, page number, and image path.
     *
     * @param id         The unique identifier for the page.
     * @param pageNumber The sequence number of the page in the document.
     * @param imagePath  The filesystem path to the stored page image.
     */
    public Page(int id, int pageNumber, String imagePath) {
        this.id = id;
        this.pageNumber = pageNumber;
        this.imagePath = imagePath;
    }

    /**
     * Gets the database ID of the page.
     *
     * @return The unique identifier of this page.
     */
    public int getId() { return id; }

    /**
     * Gets the sequence page number.
     *
     * @return The page number.
     */
    public int getPageNumber() { return pageNumber; }

    /**
     * Sets the sequence page number.
     *
     * @param pageNumber The new page number to set.
     */
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    /**
     * Gets the path to the page's image file.
     *
     * @return The image file path.
     */
    public String getImagePath() { return imagePath; }

    /**
     * Gets the rotation angle of the page image.
     *
     * @return The rotation in degrees.
     */
    public double getRotation() { return rotation; }

    /**
     * Sets the rotation angle of the page image.
     *
     * @param rotation The rotation in degrees to set.
     */
    public void setRotation(double rotation) { this.rotation = rotation; }

    /**
     * Gets the raw byte array image data.
     *
     * @return The raw image data byte array.
     */
    public byte[] getImageData() { return imageData; }

    /**
     * Sets the raw byte array image data.
     *
     * @param imageData The raw image data to set.
     */
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}
