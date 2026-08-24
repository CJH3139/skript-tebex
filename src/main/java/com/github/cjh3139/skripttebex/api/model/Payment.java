package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Payment(
    @SerializedName("txn_id") String transactionId,
    double amount,
    String currency,
    String date,
    PaymentPlayer player
) {
    public record PaymentPlayer(String name, String uuid) {}
}
