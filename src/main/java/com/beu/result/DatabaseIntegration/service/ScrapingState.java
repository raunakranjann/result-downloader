package com.beu.result.DatabaseIntegration.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ScrapingState {
    private AtomicInteger current = new AtomicInteger(0);
    private AtomicInteger total = new AtomicInteger(0);
    private boolean isRunning = false;
    private String lastLog = "";

    public void start(int totalCount) {
        this.total.set(totalCount);
        this.current.set(0);
        this.isRunning = true;
        this.lastLog = "Starting...";
    }

    public void increment(String log) {
        this.current.incrementAndGet();
        this.lastLog = log;
    }

    public void finish() {
        this.isRunning = false;
        this.lastLog = "Completed!";
    }

    // Getters
    public int getCurrent() { return current.get(); }
    public int getTotal() { return total.get(); }
    public boolean isRunning() { return isRunning; }
    public String getLastLog() { return lastLog; }
}