package com.beu.result.DocumentArchival.config;

import lombok.Data;
import java.nio.file.Paths;

/**
 * Data Transfer Object (DTO) for Archival Batch Jobs.
 * Uses Lombok to reduce boilerplate.
 */
@Data
public class ArchivalJobRequest {

    /** * The unique Key to fetch the URL from the Database.
     * Example: "5th Sem 2025 (Batch 2022-26)"
     * This acts as the identifier for both the URL and the Output Filename.
     */
    private String linkKey;

    /** Range Start: Registration ID */
    private Long rangeStart;

    /** Range End: Registration ID */
    private Long rangeEnd;

    /** * Local storage destination.
     * Can be null (will default to Documents/Academic_Archives).
     */
    private String storageLocation;

    /**
     * Custom getter to handle the default path logic.
     * Lombok will NOT generate a getter for this field since we defined one manually.
     */
    public String getStorageLocation() {
        if (storageLocation == null || storageLocation.isBlank()) {
            // Default: C:\Users\Username\Documents\Academic_Archives
            return Paths.get(System.getProperty("user.home"), "Documents", "Academic_Archives").toString();
        }
        return storageLocation;
    }
}