package com.erosmari.vividplus.utils;

import com.erosmari.vividplus.config.ConfigHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    private static ExecutorService executor;

    public static void initialize() {
        if (executor != null && !executor.isShutdown()) {
            throw new IllegalStateException("AsyncExecutor has been initialized.");
        }
        int poolSize = ConfigHandler.getInt("settings.async_thread_pool_size", 4);
        executor = Executors.newFixedThreadPool(poolSize);
    }

    public static ExecutorService getExecutor() {
        if (executor == null) {
            throw new IllegalStateException("AsyncExecutor has not been initialized.");
        }
        return executor;
    }

    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}