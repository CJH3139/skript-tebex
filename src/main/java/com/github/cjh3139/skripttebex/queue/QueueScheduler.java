package com.github.cjh3139.skripttebex.queue;

import java.time.Duration;

@FunctionalInterface
public interface QueueScheduler {
    void schedule(Runnable task, Duration delay);
}
