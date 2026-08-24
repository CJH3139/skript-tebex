package com.github.cjh3139.skripttebex.command;

import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.config.TebexConfig;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DiagnosticReport {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private DiagnosticReport() {
    }

    public static String render(TebexConfig config, TebexService service, String message) {
        String secret = config.secretKey();
        boolean connected = service != null && service.isConnected();
        int nextCheck = service != null && service.queue() != null ? service.queue().nextCheckSeconds() : -1;
        int categories = service != null ? service.cache().categories().size() : 0;
        int packages = service != null ? service.cache().packages().size() : 0;
        String store = service != null
            ? service.cache().information().map(info -> info.account().name()).orElse("<unknown>")
            : "<unknown>";

        return """
            skript-tebex diagnostic report
            generated: %s
            message: %s

            secret-key: %s
            public-token: %s
            connected: %s
            store: %s
            next check (seconds): %s
            cached categories: %s
            cached packages: %s
            """.formatted(
            LocalDateTime.now(),
            message,
            secret.isBlank() ? "<unset>" : "<redacted, " + secret.length() + " chars>",
            config.publicToken().isBlank() ? "<unset>" : "<set>",
            connected,
            store,
            nextCheck,
            categories,
            packages);
    }

    public static File write(File reportsFolder, String body) {
        try {
            Path folder = reportsFolder.toPath();
            Files.createDirectories(folder);
            Path file = folder.resolve("report-" + LocalDateTime.now().format(FILE_STAMP) + ".txt");
            Files.writeString(file, body);
            return file.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the Tebex diagnostic report", e);
        }
    }
}
