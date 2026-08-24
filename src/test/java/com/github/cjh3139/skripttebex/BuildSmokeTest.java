package com.github.cjh3139.skripttebex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildSmokeTest {
    @Test
    void mainClassNameMatchesPluginYml() {
        assertEquals("com.github.cjh3139.skripttebex.SkriptTebex", SkriptTebex.class.getName());
    }
}
