package com.github.cjh3139.skripttebex.api;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RateLimitBudget {
    private static final int DEFAULT_MAX_REQUESTS = 500;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(5);

    private static final int DEFAULT_RESERVED = 100;

    private static final Map<String, RateLimitBudget> SHARED = new ConcurrentHashMap<>();

    private final int maxRequests;
    private final int reserved;
    private final Duration window;
    private final Supplier<Instant> clock;
    private final Deque<Instant> timestamps = new ArrayDeque<>();

    public RateLimitBudget() {
        this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW, Instant::now, DEFAULT_RESERVED);
    }

    public RateLimitBudget(int maxRequests, Duration window, Supplier<Instant> clock) {
        this(maxRequests, window, clock, 0);
    }

    public RateLimitBudget(int maxRequests, Duration window, Supplier<Instant> clock, int reserved) {
        this.maxRequests = maxRequests;
        this.reserved = Math.max(0, Math.min(reserved, maxRequests));
        this.window = window;
        this.clock = clock;
    }

    public static RateLimitBudget sharedFor(String credential) {
        return SHARED.computeIfAbsent(credential == null ? "" : credential, key -> new RateLimitBudget());
    }

    public synchronized boolean tryConsume() {
        return consume(maxRequests - reserved);
    }

    public synchronized boolean tryConsumeReserved() {
        return consume(maxRequests);
    }

    private boolean consume(int limit) {
        prune();
        if (timestamps.size() >= limit) return false;
        timestamps.addLast(clock.get());
        return true;
    }

    public synchronized int remaining() {
        prune();
        return Math.max(0, (maxRequests - reserved) - timestamps.size());
    }

    public synchronized int remainingReserved() {
        prune();
        return Math.max(0, maxRequests - timestamps.size());
    }

    private void prune() {
        Instant cutoff = clock.get().minus(window);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.removeFirst();
        }
    }
}
