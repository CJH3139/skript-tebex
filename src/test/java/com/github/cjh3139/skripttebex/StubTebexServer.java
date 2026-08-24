package com.github.cjh3139.skripttebex;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StubTebexServer implements AutoCloseable {
    public record Recorded(String method, String path, String body, Map<String, String> headers) {}

    private record Canned(int status, String body) {}

    private final HttpServer server;
    private final Map<String, Canned> responses = new HashMap<>();
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    private StubTebexServer(HttpServer server) {
        this.server = server;
    }

    public static StubTebexServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        StubTebexServer stub = new StubTebexServer(server);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
            String method = exchange.getRequestMethod();
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((key, values) -> headers.put(key, values.getFirst()));
            stub.requests.add(new Recorded(method, path, body, headers));

            Canned canned = stub.responses.getOrDefault(method + " " + path, new Canned(404, "{}"));
            byte[] out = canned.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (canned.status() == 204 || out.length == 0) {
                exchange.sendResponseHeaders(canned.status(), -1);
            } else {
                exchange.sendResponseHeaders(canned.status(), out.length);
                exchange.getResponseBody().write(out);
            }
            exchange.close();
        });
        server.start();
        return stub;
    }

    public void respond(String method, String path, int status, String body) {
        responses.put(method + " " + path, new Canned(status, body));
    }

    public List<Recorded> requests() {
        return new ArrayList<>(requests);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
