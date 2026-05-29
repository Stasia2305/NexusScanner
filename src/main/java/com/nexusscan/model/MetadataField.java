package com.nexusscan.model;

/**
 * A MetadataField is a piece of information (like a "Case ID" or "Date") 
 * that users must fill in when scanning documents.
 */
public class MetadataField {
    private int id;
    private String fieldName;

    public MetadataField(int id, String fieldName) {
        this.id = id;
        this.fieldName = fieldName;
    }

    public int getId() { return id; }
    public String getFieldName() { return fieldName; }
}
