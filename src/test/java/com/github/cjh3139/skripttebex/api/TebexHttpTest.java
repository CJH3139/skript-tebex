package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.StubTebexServer;
import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class TebexHttpTest {
    record Sample(String name, int id) {}

    private StubTebexServer stub;
    private TebexHttp http;

    @BeforeEach
    void setUp() throws Exception {
        stub = StubTebexServer.start();
        http = new TebexHttp(stub.baseUrl(), Map.of("X-Tebex-Secret", "sekrit"));
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void getSendsAuthHeaderAndDeserializes() {
        stub.respond("GET", "/thing", 200, "{\"name\":\"a\",\"id\":7}");

        Sample sample = http.<Sample>get("/thing", Sample.class).join();

        assertEquals(new Sample("a", 7), sample);
        assertEquals("sekrit", stub.requests().getFirst().headers().get("X-tebex-secret"));
    }

    @Test
    void getDeserializesGenericTypes() {
        stub.respond("GET", "/things", 200, "[{\"name\":\"a\",\"id\":1},{\"name\":\"b\",\"id\":2}]");

        List<Sample> samples = http.<List<Sample>>get("/things",
            new TypeToken<List<Sample>>() {}.getType()).join();

        assertEquals(2, samples.size());
        assertEquals("b", samples.get(1).name());
    }

    @Test
    void postSerializesBody() {
        stub.respond("POST", "/thing", 201, "{\"name\":\"created\",\"id\":9}");

        Sample sample = http.<Sample>post("/thing", Map.of("package_id", "3"), Sample.class).join();

        assertEquals(9, sample.id());
        StubTebexServer.Recorded request = stub.requests().getFirst();
        assertEquals("POST", request.method());
        assertTrue(request.body().contains("\"package_id\""));
        assertEquals("application/json", request.headers().get("Content-type"));
    }

    @Test
    void deleteAcceptsNoContent() {
        stub.respond("DELETE", "/queue", 204, "");

        assertNull(http.delete("/queue", Map.of("ids", List.of(1, 2))).join());
        assertTrue(stub.requests().getFirst().body().contains("ids"));
    }

    @Test
    void errorStatusFailsWithApiException() {
        stub.respond("GET", "/thing", 429, "{\"error\":\"slow down\"}");

        CompletionException thrown = assertThrows(CompletionException.class,
            () -> http.get("/thing", Sample.class).join());

        TebexApiException cause = assertInstanceOf(TebexApiException.class, thrown.getCause());
        assertEquals(429, cause.statusCode());
        assertTrue(cause.rateLimited());
        assertFalse(cause.unauthorized());
        assertTrue(cause.body().contains("slow down"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {401, 403, 404, 500, 503})
    void mapsEveryErrorStatus(int status) {
        stub.respond("GET", "/thing", status, "{\"error\":\"nope\"}");

        CompletionException thrown = assertThrows(CompletionException.class,
            () -> http.get("/thing", Sample.class).join());

        TebexApiException cause = assertInstanceOf(TebexApiException.class, thrown.getCause());
        assertEquals(status, cause.statusCode());
        assertEquals(status == 401 || status == 403, cause.unauthorized());
        assertEquals(status >= 500, cause.serverError());
    }
}
