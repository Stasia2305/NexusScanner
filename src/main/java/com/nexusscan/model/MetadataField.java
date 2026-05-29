package com.nexusscan.model;

/**
 * Defines a custom metadata field that users must populate during scanning.
 * Fields are created by admins and appear in the metadata entry dialog.
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
