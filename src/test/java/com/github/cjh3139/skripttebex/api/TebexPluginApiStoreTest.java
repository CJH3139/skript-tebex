package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.StubTebexServer;
import com.github.cjh3139.skripttebex.api.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class TebexPluginApiStoreTest {
    private StubTebexServer stub;
    private TebexPluginApi api;

    @BeforeEach
    void setUp() throws Exception {
        stub = StubTebexServer.start();
        api = new TebexPluginApi(stub.baseUrl(), "sekrit");
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void lookupParsesPlayerAndPayments() {
        stub.respond("GET", "/user/Notch", 200, """
            {"player":{"id":1,"username":"Notch","uuid":"365bfa","plugin_username_id":11},
             "payments":[{"txn_id":"tbx-1","amount":9.99,"currency":"USD","date":"2026-01-02",
                          "player":{"name":"Notch","uuid":"365bfa"}}]}
            """);

        PlayerLookup lookup = api.lookup("Notch").join();

        assertEquals(11, lookup.player().pluginUsernameId());
        assertEquals("tbx-1", lookup.payments().getFirst().transactionId());
        assertEquals(9.99, lookup.payments().getFirst().amount(), 0.001);
    }

    @Test
    void customerPackagesUsesPlayerPath() {
        stub.respond("GET", "/player/365bfa/packages", 200,
            "{\"data\":[{\"id\":5,\"name\":\"VIP\",\"purchased_at\":\"2026-01-02\"}]}");

        List<CustomerPackage> packages = api.customerPackages("365bfa").join();

        assertEquals("VIP", packages.getFirst().name());
    }

    @Test
    void paymentsAppliesLimit() {
        stub.respond("GET", "/payments?limit=5", 200, """
            [{"txn_id":"tbx-2","amount":1.50,"currency":"GBP","date":"2026-02-02",
              "player":{"name":"7n","uuid":"ef01"}}]
            """);

        List<Payment> payments = api.payments(5).join();

        assertEquals("GBP", payments.getFirst().currency());
        assertEquals("/payments?limit=5", stub.requests().getFirst().path());
    }

    @Test
    void paymentFetchesSingleTransaction() {
        stub.respond("GET", "/payments/tbx-123", 200, """
            {"txn_id":"tbx-123","amount":4.25,"currency":"EUR","date":"2026-03-01",
             "player":{"name":"Alex","uuid":"abcd"}}
            """);

        Payment payment = api.payment("tbx-123").join();

        assertEquals("tbx-123", payment.transactionId());
        assertEquals(4.25, payment.amount(), 0.001);
        assertEquals("EUR", payment.currency());
        assertEquals("Alex", payment.player().name());
        assertEquals("/payments/tbx-123", stub.requests().getFirst().path());
    }

    @Test
    void lookupWithNullUserFailsWithoutThrowing() {
        CompletableFuture<PlayerLookup> future = assertDoesNotThrow(() -> api.lookup(null));

        assertTrue(future.isCompletedExceptionally());
        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    @Test
    void communityGoalsParsesBareArray() {
        stub.respond("GET", "/community_goals", 200, """
            [{"id":4,"name":"New spawn","description":"Fund it","target":100.0,"current":25.5,"status":"ACTIVE"}]
            """);

        List<CommunityGoal> goals = api.communityGoals().join();

        assertEquals(25.5, goals.getFirst().current(), 0.001);
        assertEquals("ACTIVE", goals.getFirst().status());
    }

    @Test
    void createBanPostsUserReasonAndIp() {
        stub.respond("POST", "/bans", 200,
            "{\"data\":{\"id\":3,\"created_at\":\"2026-03-03\",\"reason\":\"chargeback\",\"ip\":\"1.2.3.4\",\"user\":\"Notch\"}}");

        Ban ban = api.createBan("Notch", "chargeback", "1.2.3.4").join();

        assertEquals("chargeback", ban.reason());
        String body = stub.requests().getFirst().body();
        assertTrue(body.contains("\"user\""));
        assertTrue(body.contains("chargeback"));
        assertTrue(body.contains("1.2.3.4"));
    }

    @Test
    void createBanOmitsIpWhenBlank() {
        stub.respond("POST", "/bans", 200,
            "{\"data\":{\"id\":9,\"created_at\":\"2026-03-04\",\"reason\":\"spam\",\"ip\":null,\"user\":\"Notch\"}}");

        api.createBan("Notch", "chargeback", "").join();

        String body = stub.requests().getFirst().body();
        assertFalse(body.contains("\"ip\""));
    }

    @Test
    void createAndDeleteCoupon() {
        stub.respond("POST", "/coupons", 200,
            "{\"data\":{\"id\":8,\"code\":\"SUMMER\",\"effective_on\":\"cart\",\"discount\":{\"type\":\"percentage\",\"percentage\":25.0,\"value\":0.0}}}");
        stub.respond("DELETE", "/coupons/8", 204, "");

        Coupon coupon = api.createCoupon(Map.of("code", "SUMMER", "discount_percentage", 25)).join();
        assertEquals("SUMMER", coupon.code());
        assertEquals(25.0, coupon.discount().percentage(), 0.001);

        api.deleteCoupon(8).join();
        assertEquals("/coupons/8", stub.requests().get(1).path());
    }

    @Test
    void createAndVoidGiftCard() {
        stub.respond("POST", "/gift-cards", 200,
            "{\"data\":{\"id\":2,\"card_number\":\"1234\",\"balance\":10.0,\"note\":\"gift\",\"expires_at\":null,\"voided\":false}}");
        stub.respond("DELETE", "/gift-cards/2", 204, "");

        GiftCard card = api.createGiftCard(10.0, "gift", null).join();
        assertEquals("1234", card.cardNumber());
        assertFalse(card.voided());

        api.voidGiftCard(2).join();
        assertEquals("DELETE", stub.requests().get(1).method());
    }

    @Test
    void createGiftCardOmitsNoteAndExpiresAtWhenNull() {
        stub.respond("POST", "/gift-cards", 200,
            "{\"data\":{\"id\":3,\"card_number\":\"5678\",\"balance\":10.0,\"note\":null,\"expires_at\":null,\"voided\":false}}");

        api.createGiftCard(10.0, null, null).join();

        String body = stub.requests().getFirst().body();
        assertTrue(body.contains("\"amount\""));
        assertFalse(body.contains("\"note\""));
        assertFalse(body.contains("\"expires_at\""));
    }

    @Test
    void salesParsesDataEnvelope() {
        stub.respond("GET", "/sales", 200,
            "{\"data\":[{\"id\":1,\"name\":\"Winter sale\",\"effective_on\":\"package\",\"discount\":{\"type\":\"percentage\",\"percentage\":50.0,\"value\":0.0}}]}");

        List<Sale> sales = api.sales().join();

        assertEquals("Winter sale", sales.getFirst().name());
        assertEquals(50.0, sales.getFirst().discount().percentage(), 0.001);
    }
}
