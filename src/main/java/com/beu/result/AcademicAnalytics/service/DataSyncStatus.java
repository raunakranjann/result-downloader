package com.beu.result.AcademicAnalytics.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State Management Service for Data Ingestion Jobs.
 * <p>
 * This component provides real-time telemetry for the background data synchronization process.
 * It utilizes atomic variables to ensure thread-safe progress tracking suitable for
 * concurrent execution environments.
 * </p>
 */
@Component
public class DataSyncStatus {

    /**
     * Thread-safe counter for processed records.
     */
    private final AtomicInteger processedCount = new AtomicInteger(0);

    /**
     * Total number of records queued for processing.
     */
    private final AtomicInteger totalCount = new AtomicInteger(0);

    /**
     * Flag indicating if a batch job is currently active.
     */
    private volatile boolean isJobRunning = false;

    /**
     * The most recent status message or error log from the ingestion pipeline.
     */
    private volatile String operationalStatus = "";

    /**
     * Initializes a new data synchronization batch.
     * Resets counters and sets the job status to active.
     *
     * @param totalRecords The expected number of records to process.
     */
    public void startBatch(int totalRecords) {
        this.totalCount.set(totalRecords);
        this.processedCount.set(0);
        this.isJobRunning = true;
        this.operationalStatus = "Initializing Batch Ingestion Sequence...";
    }

    /**
     * Updates the progress of the current job.
     * increments the processed counter and updates the status log.
     *
     * @param statusUpdate The detailed message regarding the current operation.
     */
    public void updateProgress(String statusUpdate) {
        this.processedCount.incrementAndGet();
        this.operationalStatus = statusUpdate;
    }

    /**
     * Finalizes the batch job.
     * Marks the process as complete and logs the final status.
     */
    public void completeBatch() {
        this.isJobRunning = false;
        this.operationalStatus = "Batch Ingestion Finalized Successfully.";
    }

    // ==========================================
    // GETTERS FOR DASHBOARD MONITORING
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
        return operationalStatus;
    }
}