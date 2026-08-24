package com.github.cjh3139.skripttebex.store;

import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.ServerInformation;

import java.util.*;

public final class StoreCache {
    private volatile ServerInformation information;
    private volatile List<Category> categories = List.of();

    public void setInformation(ServerInformation information) {
        this.information = information;
    }

    public Optional<ServerInformation> information() {
        return Optional.ofNullable(information);
    }

    public void setCategories(List<Category> categories) {
        List<Category> sorted = new ArrayList<>(categories == null ? List.of() : categories);
        sorted.sort(Comparator.comparingInt(Category::order));
        this.categories = List.copyOf(sorted);
    }

    public List<Category> categories() {
        return categories;
    }

    public List<Package> packages() {
        Map<Integer, Package> byId = new LinkedHashMap<>();
        for (Category category : categories) {
            List<Package> packages = new ArrayList<>(category.packagesOrEmpty());
            packages.sort(Comparator.comparingInt(Package::order));
            for (Package pkg : packages) {
                byId.putIfAbsent(pkg.id(), pkg);
            }
        }
        return List.copyOf(byId.values());
    }

    public Optional<Package> packageById(int id) {
        return packages().stream().filter(pkg -> pkg.id() == id).findFirst();
    }

    public Optional<Package> packageByName(String name) {
        return packages().stream()
            .filter(pkg -> pkg.name() != null && pkg.name().equalsIgnoreCase(name))
            .findFirst();
    }

    public Optional<Category> categoryByName(String name) {
        return categories.stream()
            .filter(category -> name.equalsIgnoreCase(category.name()) || name.equalsIgnoreCase(category.slug()))
            .findFirst();
    }

    public void clear() {
        information = null;
        categories = List.of();
    }
}
