package com.github.cjh3139.skripttebex.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TebexConfig {
    private static final String FILE_NAME = "config.yml";

    private static final List<String> TEMPLATE = List.of(
        "secret-key: \"\"",
        "public-token: \"\"",
        "buy-gui:",
        "  enabled: true",
        "  title: \"<gold>Store\"",
        "  rows: 6",
        "verbose: false");

    private final Path file;
    private final Map<String, String> values;

    private final Object lock = new Object();

    private TebexConfig(Path file, Map<String, String> values) {
        this.file = file;
        this.values = values;
    }

    public static TebexConfig load(File dataFolder) {
        Path file = dataFolder.toPath().resolve(FILE_NAME);
        Map<String, String> values = defaults();
        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(file.getParent());
                write(file, values);
            } else {
                values.putAll(parse(Files.readAllLines(file)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + file, e);
        }
        return new TebexConfig(file, values);
    }

    private static Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("secret-key", "");
        values.put("public-token", "");
        values.put("buy-gui.enabled", "true");
        values.put("buy-gui.title", "<gold>Store");
        values.put("buy-gui.rows", "6");
        values.put("verbose", "false");
        return values;
    }

    private static Map<String, String> parse(List<String> lines) {
        Map<String, String> parsed = new LinkedHashMap<>();
        String section = "";
        for (String raw : lines) {
            String line = raw.stripTrailing();
            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;

            boolean nested = line.startsWith("  ");
            int colon = line.indexOf(':');
            if (colon < 0) continue;

            String key = line.substring(0, colon).strip();
            String value = stripComment(line.substring(colon + 1)).strip();

            if (!nested) {
                section = key;
            }

            if (value.isEmpty()) {
                continue;
            }
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = unescape(value.substring(1, value.length() - 1));
            }
            parsed.put(nested && !section.isEmpty() ? section + "." + key : key, value);
        }
        return parsed;
    }

    private static String stripComment(String rawValue) {
        String value = rawValue.strip();
        if (!value.startsWith("\"")) {
            int hash = value.indexOf('#');
            return hash >= 0 ? value.substring(0, hash) : value;
        }
        int closingQuote = closingQuoteIndex(value);
        if (closingQuote <= 0) return value;
        String afterQuote = value.substring(closingQuote + 1);
        return afterQuote.indexOf('#') >= 0 ? value.substring(0, closingQuote + 1) : value;
    }

    private static int closingQuoteIndex(String value) {
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String commentSuffix(String rawValue) {
        String value = rawValue.strip();
        int hash;
        if (value.startsWith("\"")) {
            int closingQuote = closingQuoteIndex(value);
            if (closingQuote <= 0) return "";
            hash = value.indexOf('#', closingQuote + 1);
        } else {
            hash = value.indexOf('#');
        }
        return hash < 0 ? "" : " " + value.substring(hash);
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(next == 'n' ? '\n' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String format(String value) {
        if (value == null) return "\"\"";
        if (value.equals("true") || value.equals("false") || value.matches("-?\\d+")) {
            return value;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static void write(Path file, Map<String, String> values) throws IOException {
        List<String> existing = Files.isRegularFile(file) ? Files.readAllLines(file) : TEMPLATE;
        String content = render(existing.isEmpty() ? TEMPLATE : existing, values);

        Path tmp = Files.createTempFile(file.getParent(), FILE_NAME, ".tmp");
        try {
            Files.writeString(tmp, content);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static String render(List<String> lines, Map<String, String> values) {
        StringBuilder out = new StringBuilder();
        Set<String> written = new LinkedHashSet<>();
        String section = "";

        for (String raw : lines) {
            String line = raw.stripTrailing();
            String stripped = line.stripLeading();
            int colon = line.indexOf(':');
            if (stripped.isEmpty() || stripped.startsWith("#") || colon < 0) {
                out.append(line).append('\n');
                continue;
            }

            boolean nested = line.startsWith("  ");
            String key = line.substring(0, colon).strip();
            if (!nested) {
                appendMissingNested(out, values, written, section);
                section = key;
            }

            String fullKey = nested && !section.isEmpty() ? section + "." + key : key;
            if (!values.containsKey(fullKey)) {
                out.append(line).append('\n');
                continue;
            }

            String indent = line.substring(0, line.length() - stripped.length());
            out.append(indent).append(key).append(": ").append(format(values.get(fullKey)))
                .append(commentSuffix(line.substring(colon + 1))).append('\n');
            written.add(fullKey);
        }
        appendMissingNested(out, values, written, section);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (written.contains(entry.getKey())) continue;
            int dot = entry.getKey().indexOf('.');
            if (dot < 0) {
                out.append(entry.getKey()).append(": ").append(format(entry.getValue())).append('\n');
                written.add(entry.getKey());
            } else {
                String missingSection = entry.getKey().substring(0, dot);
                out.append(missingSection).append(":\n");
                appendMissingNested(out, values, written, missingSection);
            }
        }
        return out.toString();
    }

    private static void appendMissingNested(StringBuilder out, Map<String, String> values,
                                            Set<String> written, String section) {
        if (section.isEmpty()) return;
        String prefix = section + ".";
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix) || written.contains(key)) continue;
            out.append("  ").append(key.substring(prefix.length())).append(": ")
                .append(format(entry.getValue())).append('\n');
            written.add(key);
        }
    }

    public String secretKey() {
        synchronized (lock) {
            return values.getOrDefault("secret-key", "");
        }
    }

    public boolean hasSecretKey() {
        return !secretKey().isBlank();
    }

    public String publicToken() {
        synchronized (lock) {
            return values.getOrDefault("public-token", "");
        }
    }

    public boolean buyGuiEnabled() {
        synchronized (lock) {
            return Boolean.parseBoolean(values.getOrDefault("buy-gui.enabled", "true"));
        }
    }

    public String buyGuiTitle() {
        synchronized (lock) {
            return values.getOrDefault("buy-gui.title", "<gold>Store");
        }
    }

    public int buyGuiRows() {
        synchronized (lock) {
            try {
                return Integer.parseInt(values.getOrDefault("buy-gui.rows", "6"));
            } catch (NumberFormatException e) {
                return 6;
            }
        }
    }

    public boolean verbose() {
        synchronized (lock) {
            return Boolean.parseBoolean(values.getOrDefault("verbose", "false"));
        }
    }

    public void setSecretKey(String key) {
        synchronized (lock) {
            values.put("secret-key", key == null ? "" : key);
            save();
        }
    }

    public void setPublicToken(String token) {
        synchronized (lock) {
            values.put("public-token", token == null ? "" : token);
            save();
        }
    }

    public void setVerbose(boolean verbose) {
        synchronized (lock) {
            values.put("verbose", Boolean.toString(verbose));
            save();
        }
    }

    private void save() {
        try {
            write(file, values);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }
}
