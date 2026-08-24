package com.github.cjh3139.skripttebex.store;

import com.github.cjh3139.skripttebex.api.model.Category;
import com.github.cjh3139.skripttebex.api.model.Package;
import com.github.cjh3139.skripttebex.api.model.ServerInformation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreCacheTest {
    private static Package pkg(int id, String name, int order) {
        return new Package(id, name, "", null, "single", 1.0, 1.0, "USD", 0.0, name.toLowerCase(), order);
    }

    private static Category category(int id, String name, int order, List<Package> packages) {
        return new Category(id, name, name.toLowerCase(), "", order, "grid", null, packages);
    }

    @Test
    void startsEmpty() {
        StoreCache cache = new StoreCache();

        assertTrue(cache.information().isEmpty());
        assertTrue(cache.categories().isEmpty());
        assertTrue(cache.packages().isEmpty());
        assertTrue(cache.packageById(1).isEmpty());
    }

    @Test
    void flattensPackagesInCategoryThenPackageOrder() {
        StoreCache cache = new StoreCache();
        cache.setCategories(List.of(
            category(2, "Keys", 1, List.of(pkg(20, "Rare key", 1), pkg(10, "Common key", 0))),
            category(1, "Ranks", 0, List.of(pkg(45, "VIP", 0)))));

        assertEquals(List.of(45, 10, 20), cache.packages().stream().map(Package::id).toList());
        assertEquals("VIP", cache.packageById(45).orElseThrow().name());
        assertEquals("Keys", cache.categoryByName("keys").orElseThrow().name());
    }

    @Test
    void deduplicatesPackagesAcrossCategoriesKeepingTheEarlierOrderedCategory() {
        StoreCache cache = new StoreCache();
        Package earlierCopy = pkg(99, "Earlier Copy", 0);
        Package laterCopy = pkg(99, "Later Copy", 0);
        cache.setCategories(List.of(
            category(1, "First", 0, List.of(earlierCopy)),
            category(2, "Second", 1, List.of(laterCopy))));

        List<Package> packages = cache.packages();
        assertEquals(1, packages.stream().filter(p -> p.id() == 99).count());
        assertEquals("Earlier Copy", cache.packageById(99).orElseThrow().name());
    }

    @Test
    void informationIsReadBack() {
        StoreCache cache = new StoreCache();
        cache.setInformation(new ServerInformation(
            new ServerInformation.Account(1, "http://example.tebex.io", "Example",
                new ServerInformation.Currency("USD", "$"), true),
            new ServerInformation.Server(2, "Survival")));

        assertEquals("Example", cache.information().orElseThrow().account().name());
    }
}
