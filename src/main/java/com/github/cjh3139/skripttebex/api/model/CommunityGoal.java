package com.github.cjh3139.skripttebex.api.model;

public record CommunityGoal(int id, String name, String description, double target, double current, String status) {
    public double progress() {
        return target <= 0 ? 0 : current / target;
    }
}
