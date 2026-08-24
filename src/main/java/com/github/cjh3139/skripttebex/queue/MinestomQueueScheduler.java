package com.github.cjh3139.skripttebex.queue;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MinestomQueueScheduler implements QueueScheduler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "skript-tebex-queue");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void schedule(Runnable task, Duration delay) {
        try {
            executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
