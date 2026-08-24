package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record GiftCard(
    int id,
    @SerializedName("card_number") String cardNumber,
    double balance,
    String note,
    @SerializedName("expires_at") String expiresAt,
    boolean voided
) {}
