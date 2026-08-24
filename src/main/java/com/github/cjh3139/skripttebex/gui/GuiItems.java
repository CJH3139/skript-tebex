package com.github.cjh3139.skripttebex.gui;

import com.github.cjh3139.skripttebex.api.model.Package;

import java.util.List;

public final class GuiItems {
    private GuiItems() {
    }

    public static String stripHtml(String html) {
        if (html == null) return "";
        return html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</p>", "\n")
            .replaceAll("<[^>]+>", "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .strip();
    }

    public static String priceLabel(Package pkg) {
        String total = String.format("%.2f %s", pkg.totalPrice(), pkg.currency());
        if (pkg.discount() > 0) {
            return total + String.format(" (was %.2f)", pkg.basePrice());
        }
        return total;
    }

    public static <T> List<T> paginate(List<T> items, int page, int perPage) {
        int from = page * perPage;
        if (from >= items.size()) return List.of();
        return items.subList(from, Math.min(items.size(), from + perPage));
    }
}
