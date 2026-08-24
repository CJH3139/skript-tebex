package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Basket(
    String id,
    String ident,
    boolean complete,
    String username,
    @SerializedName("total_price") double totalPrice,
    String currency,
    Links links
) {
    public record Links(String checkout, String payment) {}
}
