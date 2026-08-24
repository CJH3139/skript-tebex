package com.github.cjh3139.skripttebex.gui;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class CheckoutCooldown {
    private static final Duration DEFAULT_COOLDOWN = Duration.ofSeconds(3);

    private static final int PRUNE_THRESHOLD = 256;

    private final long cooldownNanos;
    private final LongSupplier nanoClock;
    private final Map<UUID, Long> lastAllowed = new ConcurrentHashMap<>();

    public CheckoutCooldown() {
        this(DEFAULT_COOLDOWN, System::nanoTime);
    }

    CheckoutCooldown(Duration cooldown, LongSupplier nanoClock) {
        this.cooldownNanos = cooldown.toNanos();
        this.nanoClock = nanoClock;
    }

    public boolean tryStart(UUID player) {
        long now = nanoClock.getAsLong();
        boolean[] allowed = {false};
        lastAllowed.compute(player, (key, last) -> {
            if (last != null && now - last < cooldownNanos) return last;
            allowed[0] = true;
            return now;
        });
        if (allowed[0] && lastAllowed.size() > PRUNE_THRESHOLD) {
            lastAllowed.values().removeIf(last -> now - last >= cooldownNanos);
        }
        return allowed[0];
    }
}
