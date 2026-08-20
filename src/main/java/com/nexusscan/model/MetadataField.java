package com.nexusscan.model;

/**
 * A MetadataField is a piece of information (like a "Case ID" or "Date") 
 * that users must fill in when scanning documents.
 */
public class MetadataField {
    private int id;
    private String fieldName;

    /**
     * Constructs a new MetadataField with the specified ID and field name.
     *
     * @param id        The unique identifier for the metadata field.
     * @param fieldName The name of the metadata field.
     */
    public MetadataField(int id, String fieldName) {
        this.id = id;
        this.fieldName = fieldName;
    }

    /**
     * Gets the unique identifier for the metadata field.
     *
     * @return The unique ID of the field.
     */
    public int getId() { return id; }

    /**
     * Gets the name of the metadata field.
     *
     * @return The field name.
     */
    public String getFieldName() { return fieldName; }
}
