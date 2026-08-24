package com.github.cjh3139.skripttebex.api.model;

public record CommandConditions(int delay, int slots) {
    public static final CommandConditions NONE = new CommandConditions(0, 0);
}
