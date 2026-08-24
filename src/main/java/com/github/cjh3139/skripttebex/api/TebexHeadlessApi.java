package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.Basket;
import com.github.cjh3139.skripttebex.api.model.Category;
import com.google.gson.reflect.TypeToken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class TebexHeadlessApi {
    public static final String DEFAULT_BASE_URL = "https://headless.tebex.io/api";

    private final TebexHttp http;
    private final RateLimitBudget budget;

    public TebexHeadlessApi(String publicToken) {
        this(DEFAULT_BASE_URL, publicToken);
    }

    public TebexHeadlessApi(String baseUrl, String publicToken) {
        this(baseUrl, publicToken, RateLimitBudget.sharedFor("headless:" + publicToken));
    }

    public TebexHeadlessApi(String baseUrl, String publicToken, RateLimitBudget budget) {
        String trimmedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String encodedToken = encode(publicToken);
        this.http = new TebexHttp(trimmedBaseUrl + "/accounts/" + encodedToken, Map.of());
        this.budget = budget;
    }

    protected TebexHeadlessApi() {
        this.http = null;
        this.budget = null;
    }

    public CompletableFuture<List<Category>> categories() {
        return guarded(() -> http.<Envelope<Category>>get("/categories?includePackages=1",
                new TypeToken<Envelope<Category>>() {}.getType())
            .thenApply(envelope -> envelope == null || envelope.data() == null ? List.<Category>of() : envelope.data()));
    }

    public CompletableFuture<Basket> createBasket(String username, String usernameId,
                                                  String completeUrl, String cancelUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        if (usernameId != null) body.put("username_id", usernameId);
        body.put("complete_url", completeUrl);
        body.put("cancel_url", cancelUrl);
        body.put("complete_auto_redirect", true);
        return guarded(() -> http.<Single<Basket>>post("/baskets", body, new TypeToken<Single<Basket>>() {}.getType())
            .thenApply(single -> single == null ? null : single.data()));
    }

    public CompletableFuture<Basket> addPackage(String basketIdent, int packageId, int quantity) {
        if (basketIdent == null || basketIdent.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("basketIdent must not be null or blank"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("package_id", String.valueOf(packageId));
        body.put("quantity", quantity);
        return guarded(() -> http.post("/baskets/" + encode(basketIdent) + "/packages", body, Basket.class));
    }

    public void close() {
        if (http != null) http.close();
    }

    private <T> CompletableFuture<T> guarded(Supplier<CompletableFuture<T>> call) {
        if (!budget.tryConsume()) {
            return CompletableFuture.failedFuture(new TebexApiException(429,
                "local rate limit budget exhausted, request not sent"));
        }
        return call.get();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    record Envelope<T>(List<T> data) {}

    record Single<T>(T data) {}
}
