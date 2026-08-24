package com.github.cjh3139.skripttebex.gui;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutCooldownTest {
    static final class FakeClock {
        long nanos;

        long get() {
            return nanos;
        }

        void advance(Duration amount) {
            nanos += amount.toNanos();
        }
    }

    @Test
    void secondClickInsideTheCooldownIsRefused() {
        FakeClock clock = new FakeClock();
        CheckoutCooldown cooldown = new CheckoutCooldown(Duration.ofSeconds(3), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryStart(player));

        assertFalse(cooldown.tryStart(player), "an immediate second click is refused");

        clock.advance(Duration.ofSeconds(2));
        assertFalse(cooldown.tryStart(player), "still inside the window");

        clock.advance(Duration.ofSeconds(1));
        assertTrue(cooldown.tryStart(player), "allowed again once the cooldown has passed");
    }

    @Test
    void oneImpatientPlayerDoesNotBlockAnother() {
        FakeClock clock = new FakeClock();
        CheckoutCooldown cooldown = new CheckoutCooldown(Duration.ofSeconds(3), clock::get);
        UUID spammer = UUID.randomUUID();
        UUID buyer = UUID.randomUUID();

        assertTrue(cooldown.tryStart(spammer));
        assertFalse(cooldown.tryStart(spammer));

        assertTrue(cooldown.tryStart(buyer), "the cooldown is per player, never global");
    }
}
