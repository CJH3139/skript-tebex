package com.github.cjh3139.skripttebex.api;

import com.github.cjh3139.skripttebex.api.exception.TebexApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TebexHttp {
    public static final Gson GSON = new GsonBuilder().create();

    private final String baseUrl;
    private final Map<String, String> headers;
    private final HttpClient client;

    public TebexHttp(String baseUrl, Map<String, String> headers) {
        this(baseUrl, headers, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build());
    }

    public TebexHttp(String baseUrl, Map<String, String> headers, HttpClient client) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.headers = Map.copyOf(headers);
        this.client = client;
    }

    public <T> CompletableFuture<T> get(String path, Type type) {
        return send(builder(path).GET(), type);
    }

    public <T> CompletableFuture<T> post(String path, Object body, Type type) {
        return send(builder(path).POST(publisher(body)), type);
    }

    public <T> CompletableFuture<T> put(String path, Object body, Type type) {
        return send(builder(path).PUT(publisher(body)), type);
    }

    public CompletableFuture<Void> delete(String path, Object body) {
        HttpRequest.Builder builder = builder(path);
        builder = body == null
            ? builder.DELETE()
            : builder.method("DELETE", publisher(body));
        return this.<Void>send(builder, Void.class);
    }

    private HttpRequest.BodyPublisher publisher(Object body) {
        return HttpRequest.BodyPublishers.ofString(GSON.toJson(body));
    }

    private HttpRequest.Builder builder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        return builder;
    }

    private <T> CompletableFuture<T> send(HttpRequest.Builder builder, Type type) {
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                String body = response.body();
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new TebexApiException(response.statusCode(), body);
                }
                if (type == Void.class || body == null || body.isBlank()) {
                    return null;
                }
                return GSON.fromJson(body, type);
            });
    }

    public void close() {
        client.shutdown();
    }
}
