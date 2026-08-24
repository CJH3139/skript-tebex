package com.github.cjh3139.skripttebex.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Category(
    int id,
    String name,
    String slug,
    String description,
    int order,
    @SerializedName("display_type") String displayType,
    @SerializedName("image_url") String imageUrl,
    List<Package> packages
) {
    public List<Package> packagesOrEmpty() {
        return packages == null ? List.of() : packages;
    }
}
