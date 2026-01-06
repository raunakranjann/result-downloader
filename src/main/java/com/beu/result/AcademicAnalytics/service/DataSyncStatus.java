package com.beu.result.AcademicAnalytics.service; // <--- CHANGED TO MATCH FOLDER

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DataSyncStatus {

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private volatile boolean isJobRunning = false;
    private volatile String operationalStatus = "System Idle";

    public void startJob(int totalRecords) { // <--- New Name
        this.totalCount.set(totalRecords);
        this.processedCount.set(0);
        this.isJobRunning = true;
        this.operationalStatus = "Initializing Batch Ingestion Sequence...";
    }

    public void updateProgress(String statusUpdate) {
        this.processedCount.incrementAndGet();
        this.operationalStatus = statusUpdate;
    }

    public void updateProgress(int increment, String statusUpdate) {
        this.processedCount.addAndGet(increment);
        this.operationalStatus = statusUpdate;
    }

    public void finishJob() { // <--- New Name
        this.isJobRunning = false;
        this.operationalStatus = "Batch Ingestion Finalized Successfully.";
    }

    public int getProcessedCount() { return processedCount.get(); }
    public int getTotalCount() { return totalCount.get(); }
    public boolean isJobActive() { return isJobRunning; }
    public String getOperationalStatus() { return operationalStatus; }
}