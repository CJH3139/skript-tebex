package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record PlayerLookup(Player player, java.util.List<Payment> payments) {
    public record Player(
        int id,
        String username,
        String uuid,
        @SerializedName("plugin_username_id") int pluginUsernameId
    ) {}
}
