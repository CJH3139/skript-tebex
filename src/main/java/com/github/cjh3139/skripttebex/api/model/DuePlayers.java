package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record DuePlayers(Meta meta, List<QueuedPlayer> players) {
    public record Meta(
        @SerializedName("execute_offline") boolean executeOffline,
        @SerializedName("next_check") int nextCheck,
        boolean more
    ) {}
}
