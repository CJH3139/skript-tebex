package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.StubTebexServer;
import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.github.cjh3139.skripttebex.api.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TebexPluginApiQueueTest {
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
    void duePlayersParsesMetaAndPlayers() {
        stub.respond("GET", "/queue", 200, """
            {"meta":{"execute_offline":true,"next_check":90,"more":false},
             "players":[{"id":1,"name":"Notch","uuid":"365bfa21803249ee9b634fe890c9d43f"}]}
            """);

        DuePlayers due = api.duePlayers().join();

        assertTrue(due.meta().executeOffline());
        assertEquals(90, due.meta().nextCheck());
        assertEquals(1, due.players().size());
        assertEquals("Notch", due.players().getFirst().name());
        assertEquals("365bfa21803249ee9b634fe890c9d43f", due.players().getFirst().uuid());
    }

    @Test
    void offlineCommandsParsesConditionsAndPlayer() {
        stub.respond("GET", "/queue/offline-commands", 200, """
            {"meta":{"limited":false},
             "commands":[{"id":1,"command":"say hi {name}","payment":123,"package":45,
                          "conditions":{"delay":60},
                          "player":{"id":1,"name":"Notch","uuid":"365bfa"}}]}
            """);

        List<QueuedCommand> commands = api.offlineCommands().join();

        assertEquals(1, commands.size());
        QueuedCommand command = commands.getFirst();
        assertEquals("say hi {name}", command.command());
        assertEquals(45, command.packageId());
        assertEquals(60, command.conditions().delay());
        assertEquals("Notch", command.player().name());
    }

    @Test
    void onlineCommandsUsesPluginPlayerIdInPath() {
        stub.respond("GET", "/queue/online-commands/7", 200, """
            {"commands":[{"id":2,"command":"give {name} TNT","payment":1,"package":2,
                          "conditions":{"delay":0,"slots":3}}]}
            """);

        List<QueuedCommand> commands = api.onlineCommands(7).join();

        assertEquals(1, commands.size());
        assertEquals(3, commands.getFirst().conditions().slots());
        assertEquals("/queue/online-commands/7", stub.requests().getFirst().path());
    }

    @Test
    void deleteCommandsSendsIdsArray() {
        stub.respond("DELETE", "/queue", 204, "");

        api.deleteCommands(List.of(4, 5, 6)).join();

        String body = stub.requests().getFirst().body();
        assertTrue(body.contains("\"ids\""));
        assertTrue(body.contains("4"));
        assertTrue(body.contains("6"));
    }

    @Test
    void informationParsesAccountAndServer() {
        stub.respond("GET", "/information", 200, """
            {"account":{"id":123,"domain":"http://example.tebex.io","name":"Example Store",
                        "currency":{"iso_4217":"USD","symbol":"$"},"online_mode":true},
             "server":{"id":456,"name":"Survival"}}
            """);

        ServerInformation information = api.information().join();

        assertEquals("Example Store", information.account().name());
        assertEquals("USD", information.account().currency().iso4217());
        assertEquals("Survival", information.server().name());
    }

    @Test
    void checkoutPostsPackageIdAndUsername() {
        stub.respond("POST", "/checkout", 201,
            "{\"url\":\"http://example.tebex.io/buy/gj48f\",\"expires\":\"2015-11-30T18:10:18+0000\"}");

        CheckoutUrl checkout = api.checkout(45, "Notch").join();

        assertEquals("http://example.tebex.io/buy/gj48f", checkout.url());
        String body = stub.requests().getFirst().body();

        assertTrue(body.contains("\"package_id\":\"45\""));
        assertTrue(body.contains("Notch"));
    }

    @Test
    void exhaustedBudgetReturns429AndSendsNoRequest() throws Exception {
        stub.close();
        stub = StubTebexServer.start();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-24T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(1, Duration.ofMinutes(5), now::get);
        api = new TebexPluginApi(stub.baseUrl(), "sekrit", budget);

        stub.respond("GET", "/information", 200,
            "{\"account\":{\"id\":1,\"domain\":\"x\",\"name\":\"x\",\"currency\":{\"iso_4217\":\"USD\",\"symbol\":\"$\"},\"online_mode\":true},\"server\":{\"id\":2,\"name\":\"x\"}}");

        ServerInformation info = api.information().join();
        assertNotNull(info);
        assertEquals(1, stub.requests().size(), "first call should reach the server");

        var completionException = assertThrows(java.util.concurrent.CompletionException.class,
            () -> api.information().join());
        var exception = (TebexApiException) completionException.getCause();
        assertTrue(exception.rateLimited(), "should be 429 rate limit");
        assertEquals(429, exception.statusCode());
        assertEquals(1, stub.requests().size(), "second call should NOT reach the server");
    }

    @Test
    void earlyReturnPathsDoNotConsumeBudget() throws Exception {
        stub.close();
        stub = StubTebexServer.start();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-24T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(1, Duration.ofMinutes(5), now::get);
        api = new TebexPluginApi(stub.baseUrl(), "sekrit", budget);

        stub.respond("GET", "/information", 200,
            "{\"account\":{\"id\":1,\"domain\":\"x\",\"name\":\"x\",\"currency\":{\"iso_4217\":\"USD\",\"symbol\":\"$\"},\"online_mode\":true},\"server\":{\"id\":2,\"name\":\"x\"}}");

        api.deleteCommands(List.of()).join();
        assertEquals(0, stub.requests().size(), "empty deleteCommands should not reach the server");

        ServerInformation info = api.information().join();
        assertNotNull(info);
        assertEquals(1, stub.requests().size(), "real request should reach the server");
    }

    @Test
    void deleteCommandsWithNullReturnsFailedFutureInsteadOfThrowing() {
        var future = api.deleteCommands(null);

        var exception = assertThrows(java.util.concurrent.CompletionException.class, future::join);
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals(0, stub.requests().size(), "nothing may be sent");
    }

    @Test
    void checkoutWithNullUsernameReturnsFailedFuture() {
        var future = api.checkout(45, null);

        var exception = assertThrows(java.util.concurrent.CompletionException.class, future::join);
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals(0, stub.requests().size(), "nothing may be sent");
    }

    @Test
    void scriptTrafficCannotStarveTheQueuesOwnCalls() throws Exception {
        stub.close();
        stub = StubTebexServer.start();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-24T10:00:00Z"));

        RateLimitBudget budget = new RateLimitBudget(3, Duration.ofMinutes(5), now::get, 1);
        api = new TebexPluginApi(stub.baseUrl(), "sekrit", budget);
        stub.respond("GET", "/user/Notch", 200, "{}");
        stub.respond("GET", "/queue", 200,
            "{\"meta\":{\"execute_offline\":false,\"next_check\":90,\"more\":false},\"players\":[]}");

        api.lookup("Notch").join();
        api.lookup("Notch").join();
        var starved = assertThrows(java.util.concurrent.CompletionException.class,
            () -> api.lookup("Notch").join());
        assertEquals(429, ((TebexApiException) starved.getCause()).statusCode());

        assertNotNull(api.duePlayers().join());
    }

    @Test
    void connectingCanStillAuthenticateWhenScriptTrafficHasSpentItsShare() throws Exception {
        stub.close();
        stub = StubTebexServer.start();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-24T10:00:00Z"));
        RateLimitBudget budget = new RateLimitBudget(2, Duration.ofMinutes(5), now::get, 1);
        api = new TebexPluginApi(stub.baseUrl(), "sekrit", budget);
        stub.respond("GET", "/user/Notch", 200, "{}");
        stub.respond("GET", "/information", 200,
            "{\"account\":{\"id\":1,\"domain\":\"x\",\"name\":\"x\",\"currency\":{\"iso_4217\":\"USD\",\"symbol\":\"$\"},\"online_mode\":true},\"server\":{\"id\":2,\"name\":\"x\"}}");

        api.lookup("Notch").join();
        assertThrows(java.util.concurrent.CompletionException.class, () -> api.lookup("Notch").join());

        assertNotNull(api.information().join(), "a (re)connect must still be able to authenticate");
    }
}
