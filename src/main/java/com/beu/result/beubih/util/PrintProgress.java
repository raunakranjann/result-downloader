package com.beu.result.beubih.util;

import java.util.concurrent.atomic.AtomicInteger;

public class PrintProgress {

    private static final AtomicInteger total = new AtomicInteger(0);
    private static final AtomicInteger completed = new AtomicInteger(0);
    private static volatile boolean running = false;

    public static void start(int totalCount) {
        total.set(totalCount);
        completed.set(0);
        running = true;
    }

    public static void increment() {
        completed.incrementAndGet();
    }

    public static void finish() {
        running = false;
    }

    public static int getTotal() {
        return total.get();
    }

    public static int getCompleted() {
        return completed.get();
    }

    public static boolean isRunning() {
        return running;
    }
}
