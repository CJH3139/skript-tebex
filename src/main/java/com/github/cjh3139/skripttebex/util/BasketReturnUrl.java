package com.github.cjh3139.skripttebex.util;

import com.github.cjh3139.skripttebex.TebexService;
import com.github.cjh3139.skripttebex.api.model.ServerInformation;

public final class BasketReturnUrl {
    private static final String DEFAULT = "https://tebex.io/";

    private BasketReturnUrl() {
    }

    public static String resolve(TebexService service) {
        return service.cache().information()
            .map(ServerInformation::account)
            .map(ServerInformation.Account::domain)
            .filter(domain -> domain != null && !domain.isBlank())
            .map(BasketReturnUrl::withScheme)
            .orElse(DEFAULT);
    }

    static String withScheme(String domain) {
        String trimmed = domain.strip();
        if (trimmed.regionMatches(true, 0, "http://", 0, 7)
            || trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            return trimmed;
        }

        return "https://" + (trimmed.startsWith("//") ? trimmed.substring(2) : trimmed);
    }
}
