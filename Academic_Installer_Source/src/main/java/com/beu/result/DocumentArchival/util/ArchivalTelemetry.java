package com.beu.result.DocumentArchival.util;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time monitoring component for the Document Archival Subsystem.
 */
@Component
public class ArchivalTelemetry {

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private volatile boolean isJobRunning = false;
    private volatile String currentOperation = "System Idle";

    public void initializeJob(int totalItems) {
        this.totalCount.set(totalItems);
        this.processedCount.set(0);
        this.isJobRunning = true;
        this.currentOperation = "Initializing Batch Sequence...";
    }

    /**
     * Increments the processed counter and updates the live status log.
     * UPDATED: Accepts the full status message from the Service.
     */
    public void updateStatus(String statusMessage) {
        this.processedCount.incrementAndGet();
        // Removed the hardcoded "Archived: " prefix to allow for "Skipped/Failed" messages
        this.currentOperation = statusMessage;
    }

    public void finalizeJob() {
        this.isJobRunning = false;
        this.currentOperation = "Batch Archival Completed Successfully.";
    }

    // ==========================================
    // DASHBOARD ACCESSORS
    // ==========================================

    public int getProcessedCount() { return processedCount.get(); }
    public int getTotalCount() { return totalCount.get(); }
    public boolean isJobActive() { return isJobRunning; }
    public String getOperationalStatus() { return currentOperation; }
}