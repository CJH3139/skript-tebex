package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Coupon(
    int id,
    String code,
    @SerializedName("effective_on") String effectiveOn,
    Discount discount
) {
    public record Discount(String type, double percentage, double value) {}
}
