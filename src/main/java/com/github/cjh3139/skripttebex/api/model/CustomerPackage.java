package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

public record CustomerPackage(int id, String name, @SerializedName("purchased_at") String purchasedAt) {}
