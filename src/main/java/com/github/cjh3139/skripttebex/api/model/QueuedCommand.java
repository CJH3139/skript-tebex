package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record QueuedCommand(
    int id,
    String command,
    int payment,
    @SerializedName("package") int packageId,
    CommandConditions conditions,
    QueuedPlayer player
) {
    public CommandConditions conditionsOrNone() {
        return conditions == null ? CommandConditions.NONE : conditions;
    }
}
