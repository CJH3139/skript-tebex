package com.github.cjh3139.skripttebex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TebexEventDispatcherTest {
    @Test
    void aPlainThreadIsNotTreatedAsTheTickThread() {
        assertFalse(TebexEventDispatcher.onTickThread(Thread.currentThread()));
        assertFalse(TebexEventDispatcher.onTickThread(new Thread(() -> {}, "not-a-tick-thread")));
    }
}
