package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Package(
    int id,
    String name,
    String description,
    String image,
    String type,
    @SerializedName("base_price") double basePrice,
    @SerializedName("total_price") double totalPrice,
    String currency,
    double discount,
    String slug,
    int order
) {}
