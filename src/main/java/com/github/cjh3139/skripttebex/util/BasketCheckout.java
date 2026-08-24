package com.github.cjh3139.skripttebex.util;

import com.github.cjh3139.skripttebex.api.TebexHeadlessApi;
import com.github.cjh3139.skripttebex.api.model.Basket;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BasketCheckout {
    private BasketCheckout() {
    }

    public static CompletableFuture<String> checkoutLink(TebexHeadlessApi api, String username, String usernameId,
                                                         String returnUrl, List<Integer> packageIds) {
        if (api == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Tebex has no public token configured, so no basket can be created"));
        }
        if (packageIds == null || packageIds.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("At least one package is needed to create a basket"));
        }
        return api.createBasket(username, usernameId, returnUrl, returnUrl)
            .thenCompose(created -> {
                if (created == null || created.ident() == null || created.ident().isBlank()) {
                    return CompletableFuture.<String>failedFuture(
                        new IllegalStateException("Tebex did not return a basket"));
                }
                CompletableFuture<Basket> chain = CompletableFuture.completedFuture(created);
                for (int packageId : packageIds) {
                    chain = chain.thenCompose(latest -> api.addPackage(created.ident(), packageId, 1)

                        .thenApply(updated -> checkoutOf(updated) != null ? updated : latest));
                }
                return chain.thenCompose(basket -> {
                    String checkout = checkoutOf(basket);
                    return checkout != null
                        ? CompletableFuture.completedFuture(checkout)
                        : CompletableFuture.<String>failedFuture(
                            new IllegalStateException("Tebex returned no checkout link for the basket"));
                });
            });
    }

    private static String checkoutOf(Basket basket) {
        if (basket == null || basket.links() == null) return null;
        String checkout = basket.links().checkout();
        return checkout == null || checkout.isBlank() ? null : checkout;
    }
}
