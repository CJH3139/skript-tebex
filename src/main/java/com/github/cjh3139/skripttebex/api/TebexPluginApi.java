package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.*;
import com.google.gson.reflect.TypeToken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TebexPluginApi {
    public static final String DEFAULT_BASE_URL = "https://plugin.tebex.io";

    private final TebexHttp http;
    private final RateLimitBudget budget;

    public TebexPluginApi(String secretKey) {
        this(DEFAULT_BASE_URL, secretKey);
    }

    public TebexPluginApi(String baseUrl, String secretKey) {
        this(baseUrl, secretKey, RateLimitBudget.sharedFor(secretKey));
    }

    public TebexPluginApi(String baseUrl, String secretKey, RateLimitBudget budget) {
        this.http = new TebexHttp(baseUrl, Map.of("X-Tebex-Secret", secretKey));
        this.budget = budget;
    }

    protected TebexPluginApi() {
        this.http = null;
        this.budget = null;
    }

    public CompletableFuture<ServerInformation> information() {
        return guardedReserved(() -> http.get("/information", ServerInformation.class));
    }

    public CompletableFuture<DuePlayers> duePlayers() {
        return guardedReserved(() -> http.get("/queue", DuePlayers.class));
    }

    public CompletableFuture<List<QueuedCommand>> offlineCommands() {
        return guardedReserved(() -> http.<CommandsEnvelope>get("/queue/offline-commands", CommandsEnvelope.class)
            .thenApply(CommandsEnvelope::commandsOrEmpty));
    }

    public CompletableFuture<List<QueuedCommand>> onlineCommands(int pluginPlayerId) {
        return guardedReserved(() -> http.<CommandsEnvelope>get("/queue/online-commands/" + pluginPlayerId, CommandsEnvelope.class)
            .thenApply(CommandsEnvelope::commandsOrEmpty));
    }

    public CompletableFuture<Void> deleteCommands(Collection<Integer> ids) {
        if (ids == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ids must not be null"));
        }
        if (ids.isEmpty()) return CompletableFuture.completedFuture(null);
        return guardedReserved(() -> http.delete("/queue", Map.of("ids", List.copyOf(ids))));
    }

    public CompletableFuture<CheckoutUrl> checkout(int packageId, String username) {
        if (username == null || username.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("username must not be null or blank"));
        }
        return guarded(() -> http.post("/checkout",
            Map.of("package_id", String.valueOf(packageId), "username", username),
            CheckoutUrl.class));
    }

    public CompletableFuture<PlayerLookup> lookup(String user) {
        if (user == null || user.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("user must not be null or blank"));
        }
        return guarded(() -> http.get("/user/" + encode(user), PlayerLookup.class));
    }

    public CompletableFuture<List<CustomerPackage>> customerPackages(String usernameId) {
        if (usernameId == null || usernameId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("usernameId must not be null or blank"));
        }
        return guarded(() -> unwrap(http.get("/player/" + encode(usernameId) + "/packages",
            new TypeToken<Envelope<CustomerPackage>>() {}.getType())));
    }

    public CompletableFuture<List<Payment>> payments(int limit) {
        return guarded(() -> http.get("/payments?limit=" + limit, new TypeToken<List<Payment>>() {}.getType()));
    }

    public CompletableFuture<Payment> payment(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("transactionId must not be null or blank"));
        }
        return guarded(() -> http.get("/payments/" + encode(transactionId), Payment.class));
    }

    public CompletableFuture<List<CommunityGoal>> communityGoals() {
        return guarded(() -> http.get("/community_goals", new TypeToken<List<CommunityGoal>>() {}.getType()));
    }

    public CompletableFuture<List<Ban>> bans() {
        return guarded(() -> unwrap(http.get("/bans", new TypeToken<Envelope<Ban>>() {}.getType())));
    }

    public CompletableFuture<Ban> createBan(String user, String reason, String ip) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("reason", reason == null ? "" : reason);
        if (ip != null && !ip.isBlank()) body.put("ip", ip);
        return guarded(() -> unwrapSingle(http.post("/bans", body, new TypeToken<Single<Ban>>() {}.getType())));
    }

    public CompletableFuture<List<Coupon>> coupons() {
        return guarded(() -> unwrap(http.get("/coupons", new TypeToken<Envelope<Coupon>>() {}.getType())));
    }

    public CompletableFuture<Coupon> createCoupon(Map<String, Object> request) {
        return guarded(() -> unwrapSingle(http.post("/coupons", request, new TypeToken<Single<Coupon>>() {}.getType())));
    }

    public CompletableFuture<Void> deleteCoupon(int id) {
        return guarded(() -> http.delete("/coupons/" + id, null));
    }

    public CompletableFuture<List<GiftCard>> giftCards() {
        return guarded(() -> unwrap(http.get("/gift-cards", new TypeToken<Envelope<GiftCard>>() {}.getType())));
    }

    public CompletableFuture<GiftCard> createGiftCard(double amount, String note, String expiresAt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        if (note != null) body.put("note", note);
        if (expiresAt != null) body.put("expires_at", expiresAt);
        return guarded(() -> unwrapSingle(http.post("/gift-cards", body, new TypeToken<Single<GiftCard>>() {}.getType())));
    }

    public CompletableFuture<Void> voidGiftCard(int id) {
        return guarded(() -> http.delete("/gift-cards/" + id, null));
    }

    public CompletableFuture<List<Sale>> sales() {
        return guarded(() -> unwrap(http.get("/sales", new TypeToken<Envelope<Sale>>() {}.getType())));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static <T> CompletableFuture<List<T>> unwrap(CompletableFuture<Envelope<T>> future) {
        return future.thenApply(envelope -> envelope == null || envelope.data() == null ? List.of() : envelope.data());
    }

    private static <T> CompletableFuture<T> unwrapSingle(CompletableFuture<Single<T>> future) {
        return future.thenApply(single -> single == null ? null : single.data());
    }

    record Envelope<T>(List<T> data) {}

    record Single<T>(T data) {}

    protected TebexHttp http() {
        return http;
    }

    record CommandsEnvelope(List<QueuedCommand> commands) {
        List<QueuedCommand> commandsOrEmpty() {
            return commands == null ? List.of() : commands;
        }
    }

    protected <T> CompletableFuture<T> guarded(java.util.function.Supplier<CompletableFuture<T>> call) {
        return guarded(call, false);
    }

    protected <T> CompletableFuture<T> guardedReserved(java.util.function.Supplier<CompletableFuture<T>> call) {
        return guarded(call, true);
    }

    private <T> CompletableFuture<T> guarded(java.util.function.Supplier<CompletableFuture<T>> call, boolean reserved) {
        boolean allowed = reserved ? budget.tryConsumeReserved() : budget.tryConsume();
        if (!allowed) {
            return CompletableFuture.failedFuture(new TebexApiException(429,
                "local rate limit budget exhausted, request not sent"));
        }
        return call.get();
    }

    public void close() {
        if (http != null) http.close();
    }
}
