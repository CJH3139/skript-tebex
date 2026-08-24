package com.github.cjh3139.skripttebex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasketReturnUrlTest {
    @Test
    void bareHostGetsHttpsPrefix() {
        assertEquals("https://store.example.com", BasketReturnUrl.withScheme("store.example.com"));
    }

    @Test
    void existingSchemeIsLeftAlone() {
        assertEquals("https://store.example.com", BasketReturnUrl.withScheme("https://store.example.com"));
        assertEquals("http://store.example.com", BasketReturnUrl.withScheme("http://store.example.com"),
            "a deliberate http:// store is not silently upgraded");
        assertEquals("HTTPS://store.example.com", BasketReturnUrl.withScheme("HTTPS://store.example.com"),
            "the scheme check is case-insensitive, as schemes are");
    }

    @Test
    void protocolRelativeAndPaddedValuesAreNormalised() {
        assertEquals("https://store.example.com", BasketReturnUrl.withScheme("//store.example.com"));
        assertEquals("https://store.example.com", BasketReturnUrl.withScheme("  store.example.com  "));
        assertEquals("https://store.example.com/buy", BasketReturnUrl.withScheme("store.example.com/buy"));
    }
}
