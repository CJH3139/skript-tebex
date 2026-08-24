package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Sale(
    int id,
    String name,
    @SerializedName("effective_on") String effectiveOn,
    Coupon.Discount discount
) {}
