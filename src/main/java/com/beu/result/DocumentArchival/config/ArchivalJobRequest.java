package com.beu.result.DocumentArchival.config;

import java.nio.file.Paths;

/**
 * Data Transfer Object (DTO) representing the parameters for a specific
 * Document Archival Batch Job.
 * <p>
 * This class captures the criteria (Semester, Session, Range) required to
 * generate the target URLs for digital record preservation.
 * </p>
 */
public class ArchivalJobRequest {

    /** Target Academic Semester (e.g., "I", "V", "VII") */
    private String targetSemester;

    /** The Academic Session Year (e.g., 2024) */
    private Integer academicSession;

    /** * The official publication reference (e.g., "July/2025").
     * Critical for constructing valid query parameters.
     */
    private String publicationCycle;

    /** Range Start: Registration ID */
    private Long rangeStart;

    /** Range End: Registration ID */
    private Long rangeEnd;

    /** * Local storage destination for the generated archives.
     * Defaults to the user's "Downloads" or "Documents" folder.
     */
    private String storageLocation;

    // ==========================================
    // ACCESSORS & MUTATORS (Getters/Setters)
    // ==========================================

    public String getTargetSemester() {
        return (targetSemester == null || targetSemester.isBlank()) ? "I" : targetSemester;
    }

    public void setTargetSemester(String targetSemester) {
        this.targetSemester = targetSemester;
    }

    public Integer getAcademicSession() {
        return (academicSession == null) ? 2024 : academicSession;
    }

    public void setAcademicSession(Integer academicSession) {
        this.academicSession = academicSession;
    }

    public String getPublicationCycle() {
        return (publicationCycle == null || publicationCycle.isBlank())
                ? "July/2025"
                : publicationCycle.trim();
    }

    public void setPublicationCycle(String publicationCycle) {
        this.publicationCycle = publicationCycle;
    }

    public Long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(Long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public Long getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(Long rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    /**
     * Resolves the output directory.
     * Uses the System User Home as a safe fallback to avoid OS-specific path errors.
     */
    public String getStorageLocation() {
        if (storageLocation == null || storageLocation.isBlank()) {
            // Default to: C:\Users\Username\Documents\Academic_Archives
            return Paths.get(System.getProperty("user.home"), "Documents", "Academic_Archives").toString();
        }
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
}