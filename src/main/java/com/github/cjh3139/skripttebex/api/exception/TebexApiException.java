package com.github.cjh3139.skripttebex.api.exception;

public class TebexApiException extends RuntimeException {
    private final int statusCode;
    private final String body;

    public TebexApiException(int statusCode, String body) {
        super("Tebex API returned HTTP " + statusCode + ": " + body);
        this.statusCode = statusCode;
        this.body = body;
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }

    public boolean rateLimited() {
        return statusCode == 429;
    }

    public boolean unauthorized() {
        return statusCode == 401 || statusCode == 403;
    }

    public boolean serverError() {
        return statusCode >= 500;
    }
}
