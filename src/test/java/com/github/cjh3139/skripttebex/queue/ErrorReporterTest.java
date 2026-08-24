package com.github.cjh3139.skripttebex.queue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorReporterTest {
    @Test
    void reportsEachKeyOnceUntilCleared() {
        List<String> logged = new ArrayList<>();
        ErrorReporter reporter = new ErrorReporter((message, cause) -> logged.add(message));

        reporter.report("queue", "queue failed", null);
        reporter.report("queue", "queue failed", null);
        reporter.report("listing", "listing failed", null);

        assertEquals(List.of("queue failed", "listing failed"), logged);

        reporter.clear("queue");
        reporter.report("queue", "queue failed again", null);

        assertEquals(List.of("queue failed", "listing failed", "queue failed again"), logged);
    }
}
