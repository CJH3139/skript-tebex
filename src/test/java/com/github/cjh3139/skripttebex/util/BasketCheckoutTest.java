package com.github.cjh3139.skripttebex.util;

import com.github.cjh3139.skripttebex.api.TebexHeadlessApi;
import com.github.cjh3139.skripttebex.api.model.Basket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class BasketCheckoutTest {
    static class FakeHeadlessApi extends TebexHeadlessApi {
        Basket created = basket("abc123", "https://checkout.tebex.io/abc123");
        RuntimeException failCreateWith;
        RuntimeException failAddWith;

        final List<Integer> added = new ArrayList<>();

        Basket addResponse;

        @Override
        public CompletableFuture<Basket> createBasket(String username, String usernameId,
                                                      String completeUrl, String cancelUrl) {
            if (failCreateWith != null) return CompletableFuture.failedFuture(failCreateWith);
            return CompletableFuture.completedFuture(created);
        }

        @Override
        public CompletableFuture<Basket> addPackage(String basketIdent, int packageId, int quantity) {
            added.add(packageId);
            if (failAddWith != null) return CompletableFuture.failedFuture(failAddWith);
            return CompletableFuture.completedFuture(addResponse);
        }
    }

    private static Basket basket(String ident, String checkout) {
        return new Basket("id", ident, false, "Notch", 0.0, "USD",
            checkout == null ? null : new Basket.Links(checkout, null));
    }

    @Test
    void addsEveryPackageInOrderAndReturnsTheCheckoutLink() {
        FakeHeadlessApi api = new FakeHeadlessApi();
        api.addResponse = basket("abc123", "https://checkout.tebex.io/final");

        String checkout = BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of(45, 46)).join();

        assertEquals(List.of(45, 46), api.added);
        assertEquals("https://checkout.tebex.io/final", checkout);
    }

    @Test
    void fallsBackToTheCreatedBasketsLinkWhenAddPackageDoesNotCarryOne() {
        FakeHeadlessApi api = new FakeHeadlessApi();
        api.addResponse = basket("abc123", null);

        String checkout = BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of(45)).join();

        assertEquals("https://checkout.tebex.io/abc123", checkout);
    }

    @Test
    void missingCheckoutLinkFailsWithAClearMessageInsteadOfNullPointer() {
        FakeHeadlessApi api = new FakeHeadlessApi();
        api.created = basket("abc123", null);
        api.addResponse = basket("abc123", null);

        var exception = assertThrows(CompletionException.class, () -> BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of(45)).join());

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("Tebex returned no checkout link for the basket", exception.getCause().getMessage());
    }

    @Test
    void missingBasketFailsBeforeAnyPackageIsAdded() {
        FakeHeadlessApi api = new FakeHeadlessApi();
        api.created = null;

        var exception = assertThrows(CompletionException.class, () -> BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of(45)).join());

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertTrue(api.added.isEmpty());
    }

    @Test
    void aFailedAddStopsBeforeTheNextPackage() {
        FakeHeadlessApi api = new FakeHeadlessApi();
        api.failAddWith = new IllegalStateException("nope");

        assertThrows(CompletionException.class, () -> BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of(45, 46)).join());

        assertEquals(List.of(45), api.added, "a half-built basket is abandoned, not completed");
    }

    @Test
    void noApiAndNoPackagesFailWithoutCallingTebex() {
        assertThrows(CompletionException.class, () -> BasketCheckout
            .checkoutLink(null, "Notch", "365bfa", "https://store.example.com", List.of(45)).join());

        FakeHeadlessApi api = new FakeHeadlessApi();
        assertThrows(CompletionException.class, () -> BasketCheckout
            .checkoutLink(api, "Notch", "365bfa", "https://store.example.com", List.of()).join());
        assertTrue(api.added.isEmpty());
    }
}
