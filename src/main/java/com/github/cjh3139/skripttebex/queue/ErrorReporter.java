package com.github.cjh3139.skripttebex.queue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ErrorReporter {
    private final BiConsumer<String, Throwable> sink;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    public ErrorReporter(BiConsumer<String, Throwable> sink) {
        this.sink = sink;
    }

    public void report(String key, String message, Throwable cause) {
        if (!reported.add(key)) return;
        sink.accept(message, cause);
    }

    public void clear(String key) {
        reported.remove(key);
    }

    public void clearAll() {
        reported.clear();
    }
}
