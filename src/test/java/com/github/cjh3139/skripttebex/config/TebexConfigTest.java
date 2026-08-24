package com.github.cjh3139.skripttebex.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TebexConfigTest {
    @Test
    void writesDefaultsWhenMissing(@TempDir Path dir) {
        TebexConfig config = TebexConfig.load(dir.toFile());

        assertTrue(new File(dir.toFile(), "config.yml").isFile());
        assertEquals("", config.secretKey());
        assertEquals("", config.publicToken());
        assertFalse(config.hasSecretKey());
        assertTrue(config.buyGuiEnabled());
        assertEquals("<gold>Store", config.buyGuiTitle());
        assertEquals(6, config.buyGuiRows());
        assertFalse(config.verbose());
    }

    @Test
    void readsExistingValues(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: "abc123"
            public-token: "t-public"
            buy-gui:
              enabled: false
              title: "<red>Shop"
              rows: 3
            verbose: true
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());

        assertEquals("abc123", config.secretKey());
        assertEquals("t-public", config.publicToken());
        assertTrue(config.hasSecretKey());
        assertFalse(config.buyGuiEnabled());
        assertEquals("<red>Shop", config.buyGuiTitle());
        assertEquals(3, config.buyGuiRows());
        assertTrue(config.verbose());
    }

    @Test
    void persistsSecretKeyAndSurvivesReload(@TempDir Path dir) {
        TebexConfig config = TebexConfig.load(dir.toFile());
        config.setSecretKey("newsecret");

        TebexConfig reloaded = TebexConfig.load(dir.toFile());
        assertEquals("newsecret", reloaded.secretKey());
        assertEquals("<gold>Store", reloaded.buyGuiTitle());
    }

    @Test
    void sectionHeaderWithTrailingComment(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: "abc123"
            public-token: "t-public"
            buy-gui: # settings section
              enabled: false
              title: "<red>Shop"
              rows: 3
            verbose: true
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());

        assertEquals("abc123", config.secretKey());
        assertFalse(config.buyGuiEnabled());
        assertEquals("<red>Shop", config.buyGuiTitle());
        assertEquals(3, config.buyGuiRows());
    }

    @Test
    void inlineTrailingComment(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: "xyz789" # the api key
            public-token: "t-pub" # token for requests
            buy-gui:
              enabled: true # enable or disable the gui
              title: "<gold>Store" # store title
              rows: 6 # number of rows
            verbose: false # debug mode
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());

        assertEquals("xyz789", config.secretKey());
        assertEquals("t-pub", config.publicToken());
        assertTrue(config.buyGuiEnabled());
        assertEquals("<gold>Store", config.buyGuiTitle());
        assertEquals(6, config.buyGuiRows());
        assertFalse(config.verbose());
    }

    @Test
    void hashInsideQuotedTitle(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: ""
            public-token: ""
            buy-gui:
              enabled: true
              title: "<gold>#1 Best Store"
              rows: 6
            verbose: false
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());
        assertEquals("<gold>#1 Best Store", config.buyGuiTitle());

        config.setSecretKey("test");
        TebexConfig reloaded = TebexConfig.load(dir.toFile());
        assertEquals("<gold>#1 Best Store", reloaded.buyGuiTitle());
    }

    @Test
    void blankLinesAndCommentsIgnored(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            # This is a comment at the top
            secret-key: "key123"

            # Comment before section
            public-token: "token456"

            buy-gui:
              # Nested comment
              enabled: true
              # Another comment
              title: "<gold>Store"
              # Yet another
              rows: 6

            # End comment
            verbose: false
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());

        assertEquals("key123", config.secretKey());
        assertEquals("token456", config.publicToken());
        assertTrue(config.buyGuiEnabled());
        assertEquals("<gold>Store", config.buyGuiTitle());
        assertEquals(6, config.buyGuiRows());
        assertFalse(config.verbose());
    }

    @Test
    void lineWithoutColonIgnored(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: "key789"
            invalid line without colon
            public-token: "token789"
            buy-gui:
              enabled: true
              title: "<gold>Store"
              rows: 6
            verbose: false
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());

        assertEquals("key789", config.secretKey());
        assertEquals("token789", config.publicToken());
        assertTrue(config.buyGuiEnabled());
    }

    @Test
    void savePreservesCommentsBlankLinesAndUnknownKeys(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            # skript-tebex configuration
            secret-key: "abc123" # do not share this

            # store front
            buy-gui:
              enabled: false
              title: "<red>Shop"
              rows: 3
            public-token: "t-public"
            verbose: true
            some-future-key: "keep me"
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());
        config.setVerbose(false);

        String saved = Files.readString(dir.resolve("config.yml"));
        assertTrue(saved.contains("# skript-tebex configuration"), "operator comments survive a save");
        assertTrue(saved.contains("# do not share this"), "trailing comments survive a save");
        assertTrue(saved.contains("# store front"));
        assertTrue(saved.contains("some-future-key: \"keep me\""), "unknown keys are not deleted");
        assertTrue(saved.contains("verbose: false"), "the changed value is written");

        TebexConfig reloaded = TebexConfig.load(dir.toFile());
        assertEquals("abc123", reloaded.secretKey());
        assertEquals("t-public", reloaded.publicToken());
        assertEquals("<red>Shop", reloaded.buyGuiTitle());
        assertEquals(3, reloaded.buyGuiRows());
        assertFalse(reloaded.buyGuiEnabled());
        assertFalse(reloaded.verbose());
    }

    @Test
    void quotesInAValueSurviveTheRoundTripInsteadOfCorruptingTheFile(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            secret-key: ""
            public-token: ""
            buy-gui:
              enabled: true
              title: "<gold>Store"
              rows: 6
            verbose: false
            """);
        TebexConfig config = TebexConfig.load(dir.toFile());

        config.setSecretKey("say \"hi\" # now");

        TebexConfig reloaded = TebexConfig.load(dir.toFile());
        assertEquals("say \"hi\" # now", reloaded.secretKey(), "the value must round-trip exactly");
        assertEquals("<gold>Store", reloaded.buyGuiTitle(), "and must not corrupt its neighbours");
        assertEquals(6, reloaded.buyGuiRows());
        assertFalse(reloaded.verbose());
    }

    @Test
    void backslashesInAValueRoundTrip(@TempDir Path dir) {
        TebexConfig config = TebexConfig.load(dir.toFile());

        config.setPublicToken("a\\b\"c");

        assertEquals("a\\b\"c", TebexConfig.load(dir.toFile()).publicToken());
    }

    @Test
    void keysMissingFromAnOperatorsFileAreAddedBackOnSave(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            # a hand-trimmed file
            secret-key: "abc123"
            """);

        TebexConfig config = TebexConfig.load(dir.toFile());
        config.setVerbose(true);

        TebexConfig reloaded = TebexConfig.load(dir.toFile());
        assertEquals("abc123", reloaded.secretKey());
        assertTrue(reloaded.verbose());
        assertEquals("<gold>Store", reloaded.buyGuiTitle(), "defaults for missing keys are written out");
        assertEquals(6, reloaded.buyGuiRows());
        assertTrue(Files.readString(dir.resolve("config.yml")).contains("# a hand-trimmed file"));
    }
}
