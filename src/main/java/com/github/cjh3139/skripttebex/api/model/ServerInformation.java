package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record ServerInformation(Account account, Server server) {
    public record Account(
        int id,
        String domain,
        String name,
        Currency currency,
        @SerializedName("online_mode") boolean onlineMode
    ) {}

    public record Currency(@SerializedName("iso_4217") String iso4217, String symbol) {}

    public record Server(int id, String name) {}
}
