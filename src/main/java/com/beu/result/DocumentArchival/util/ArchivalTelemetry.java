package com.beu.result.DocumentArchival.util;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time monitoring component for the Document Archival Subsystem.
 * <p>
 * This class maintains thread-safe counters and status flags to provide
 * live feedback to the administrative console during batch processing.
 * </p>
 */
@Component
public class ArchivalTelemetry {

    /** Thread-safe counter for processed records */
    private final AtomicInteger processedCount = new AtomicInteger(0);

    /** Total records in the current batch queue */
    private final AtomicInteger totalCount = new AtomicInteger(0);

    /** Global flag indicating if the archival engine is busy */
    private volatile boolean isJobRunning = false;

    /** Live log message displayed on the dashboard */
    private volatile String currentOperation = "System Idle";

    /**
     * Resets telemetry for a new batch job.
     * @param totalItems The expected number of records to process.
     */
    public void initializeJob(int totalItems) {
        this.totalCount.set(totalItems);
        this.processedCount.set(0);
        this.isJobRunning = true;
        this.currentOperation = "Initializing Batch Sequence...";
    }

    /**
     * Increments the processed counter and updates the live status log.
     * @param artifactName The name of the file currently being processed.
     */
    public void updateStatus(String artifactName) {
        this.processedCount.incrementAndGet();
        this.currentOperation = "Archived: " + artifactName;
    }

    /**
     * Marks the current job as complete.
     */
    public void finalizeJob() {
        this.isJobRunning = false;
        this.currentOperation = "Batch Archival Completed Successfully.";
    }

    // ==========================================
    // DASHBOARD ACCESSORS (JSON Serialization)
    // ==========================================

    public int getProcessedCount() {
        return processedCount.get();
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public boolean isJobActive() {
        return isJobRunning;
    }

    public String getOperationalStatus() {
        return currentOperation;
    }
}