package com.github.cjh3139.skripttebex.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitBudgetTest {
    @Test
    void allowsUpToTheLimitThenRefusesWithinTheWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-23T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(3, Duration.ofMinutes(5), now::get);

        assertTrue(budget.tryConsume());
        assertTrue(budget.tryConsume());
        assertTrue(budget.tryConsume());
        assertFalse(budget.tryConsume());
        assertEquals(0, budget.remaining());
    }

    @Test
    void freesCapacityOnceTheWindowPasses() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-23T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(1, Duration.ofMinutes(5), now::get);

        assertTrue(budget.tryConsume());
        assertFalse(budget.tryConsume());

        now.set(Instant.parse("2026-08-23T10:05:01Z"));
        assertTrue(budget.tryConsume());
    }

    @Test
    void freesCapacityWhenTimestampEqualsWindowBoundary() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-23T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(1, Duration.ofMinutes(5), now::get);

        assertTrue(budget.tryConsume());
        assertFalse(budget.tryConsume());

        now.set(Instant.parse("2026-08-23T10:05:00Z"));
        assertTrue(budget.tryConsume(), "entry at exact boundary should be freed");
    }

    @Test
    void reservedHeadroomIsOnlySpendableByTheQueue() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-23T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(5, Duration.ofMinutes(5), now::get, 2);

        assertTrue(budget.tryConsume());
        assertTrue(budget.tryConsume());
        assertTrue(budget.tryConsume());
        assertFalse(budget.tryConsume(), "script traffic stops at the reserved boundary");
        assertEquals(0, budget.remaining());

        assertEquals(2, budget.remainingReserved());
        assertTrue(budget.tryConsumeReserved());
        assertTrue(budget.tryConsumeReserved());
        assertFalse(budget.tryConsumeReserved(), "the hard limit is still the hard limit");
    }

    @Test
    void sharedBudgetIsTheSameInstanceForOneCredentialAndSurvivesRebuilds() {
        RateLimitBudget first = RateLimitBudget.sharedFor("secret-a");
        RateLimitBudget again = RateLimitBudget.sharedFor("secret-a");
        RateLimitBudget other = RateLimitBudget.sharedFor("secret-b");

        assertSame(first, again, "a reconnect must not hand out a fresh allowance");
        assertNotSame(first, other, "different credentials are limited separately");

        int before = first.remaining();
        assertTrue(first.tryConsume());
        assertEquals(before - 1, RateLimitBudget.sharedFor("secret-a").remaining(),
            "spending through one client is visible to the next client built for the same secret");
    }
}
