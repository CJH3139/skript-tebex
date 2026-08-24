package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.StubTebexServer;
import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.Basket;
import com.github.cjh3139.skripttebex.api.model.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TebexHeadlessApiTest {
    private StubTebexServer stub;
    private TebexHeadlessApi api;

    @BeforeEach
    void setUp() throws Exception {
        stub = StubTebexServer.start();
        api = new TebexHeadlessApi(stub.baseUrl(), "t-public");
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void categoriesIncludePackagesAndScopeToken() {
        stub.respond("GET", "/accounts/t-public/categories?includePackages=1", 200, """
            {"data":[{"id":1,"name":"Ranks","slug":"ranks","description":"<p>Ranks</p>","order":0,
                      "display_type":"grid","image_url":null,
                      "packages":[{"id":45,"name":"VIP","description":"<p>VIP</p>","image":null,
                                   "type":"single","base_price":9.99,"total_price":9.99,
                                   "currency":"USD","discount":0.0,"slug":"vip","order":0}]}]}
            """);

        List<Category> categories = api.categories().join();

        assertEquals(1, categories.size());
        assertEquals("Ranks", categories.getFirst().name());
        assertEquals("VIP", categories.getFirst().packagesOrEmpty().getFirst().name());
        assertEquals(9.99, categories.getFirst().packagesOrEmpty().getFirst().totalPrice(), 0.001);
    }

    @Test
    void createBasketSendsUsernameAndUrls() {
        stub.respond("POST", "/accounts/t-public/baskets", 200, """
            {"data":{"id":"b1","ident":"abc123","complete":false,"username":"Notch",
                     "total_price":0.0,"currency":"USD",
                     "links":{"checkout":"https://checkout.tebex.io/abc123"}}}
            """);

        Basket basket = api.createBasket("Notch", "365bfa",
            "https://example.com/done", "https://example.com/cancel").join();

        assertEquals("abc123", basket.ident());
        assertEquals("https://checkout.tebex.io/abc123", basket.links().checkout());
        String body = stub.requests().getFirst().body();
        assertTrue(body.contains("complete_url"));
        assertTrue(body.contains("cancel_url"));
        assertTrue(body.contains("Notch"));
        assertTrue(body.contains("username_id"));
        assertTrue(body.contains("complete_auto_redirect"));
    }

    @Test
    void createBasketWithNullUsernameIdOmitsKey() {
        stub.respond("POST", "/accounts/t-public/baskets", 200, """
            {"data":{"id":"b1","ident":"abc123","complete":false,"username":"Steve",
                     "total_price":0.0,"currency":"USD",
                     "links":{"checkout":"https://checkout.tebex.io/abc123"}}}
            """);

        Basket basket = api.createBasket("Steve", null,
            "https://example.com/done", "https://example.com/cancel").join();

        assertEquals("abc123", basket.ident());
        String body = stub.requests().getFirst().body();
        assertFalse(body.contains("username_id"));
    }

    @Test
    void addPackageUsesBasketIdentPath() {
        stub.respond("POST", "/accounts/t-public/baskets/abc123/packages", 200, """
            {"id":"b1","ident":"abc123","complete":false,"username":"Notch",
             "total_price":9.99,"currency":"USD",
             "links":{"checkout":"https://checkout.tebex.io/abc123"}}
            """);

        Basket basket = api.addPackage("abc123", 45, 1).join();

        assertEquals(9.99, basket.totalPrice(), 0.001);
        String body = stub.requests().getFirst().body();
        assertTrue(body.contains("\"package_id\":\"45\""));
        assertTrue(body.contains("\"quantity\""));
    }

    @Test
    void addPackageWithNullBasketIdentReturnsFailedFuture() {
        var future = api.addPackage(null, 45, 1);

        var exception = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("basketIdent must not be null or blank", exception.getCause().getMessage());
    }

    @Test
    void exhaustedBudgetRefusesFurtherHeadlessCallsWithoutSendingThem() throws Exception {
        stub.close();
        stub = StubTebexServer.start();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-24T10:00:00Z"));

        api = new TebexHeadlessApi(stub.baseUrl(), "t-public",
            new RateLimitBudget(1, Duration.ofMinutes(5), now::get));
        stub.respond("POST", "/accounts/t-public/baskets", 200, """
            {"data":{"id":"b1","ident":"abc123","complete":false,"username":"Notch",
                     "total_price":0.0,"currency":"USD",
                     "links":{"checkout":"https://checkout.tebex.io/abc123"}}}
            """);

        assertNotNull(api.createBasket("Notch", "365bfa", "https://x/", "https://x/").join());
        assertEquals(1, stub.requests().size());

        var exception = assertThrows(CompletionException.class,
            () -> api.addPackage("abc123", 45, 1).join());
        assertEquals(429, ((TebexApiException) exception.getCause()).statusCode());
        assertEquals(1, stub.requests().size(), "the refused call must not reach Tebex");
    }

    @Test
    void budgetIsSharedByEveryClientBuiltForOnePublicToken() {
        RateLimitBudget first = RateLimitBudget.sharedFor("headless:shared-token");
        int before = first.remaining();

        assertTrue(first.tryConsume());

        assertEquals(before - 1, RateLimitBudget.sharedFor("headless:shared-token").remaining(),
            "rebuilding the client on reconnect must not hand back a fresh allowance");
    }
}
