package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record Ban(int id, @SerializedName("created_at") String createdAt, String reason, String ip, String user) {}
