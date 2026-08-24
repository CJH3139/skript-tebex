package com.github.cjh3139.skripttebex.command;

import com.github.cjh3139.skripttebex.config.TebexConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticReportTest {
    @Test
    void redactsTheSecretKey(@TempDir Path dir) {
        TebexConfig config = TebexConfig.load(dir.toFile());
        config.setSecretKey("super-secret-value");

        String body = DiagnosticReport.render(config, null, "queue looks stuck");

        assertFalse(body.contains("super-secret-value"));
        assertTrue(body.contains("secret-key: <redacted, 18 chars>"));
        assertTrue(body.contains("queue looks stuck"));
        assertTrue(body.contains("connected: false"));
    }

    @Test
    void writesTimestampedFile(@TempDir Path dir) throws Exception {
        File written = DiagnosticReport.write(dir.toFile(), "body text");

        assertTrue(written.isFile());
        assertTrue(written.getName().startsWith("report-"));
        assertTrue(written.getName().endsWith(".txt"));
        assertEquals("body text", Files.readString(written.toPath()));
    }
}
