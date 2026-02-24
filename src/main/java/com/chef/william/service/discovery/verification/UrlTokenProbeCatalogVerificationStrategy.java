package com.chef.william.service.discovery.verification;

import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Component
public class UrlTokenProbeCatalogVerificationStrategy implements CatalogVerificationStrategy {

    @Override
    public CatalogVerificationResult verify(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return CatalogVerificationResult.noMatch("URL_TOKEN_PROBE", "INVALID_INPUT");
        }

        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        boolean allMatched = Arrays.stream(ingredientName.toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(token -> !token.isBlank())
                .allMatch(decoded::contains);

        if (allMatched) {
            return CatalogVerificationResult.matched("CATALOG_SEARCH_URL_TEMPLATE", 0.60);
        }

        return CatalogVerificationResult.noMatch("URL_TOKEN_PROBE", "INGREDIENT_TOKENS_NOT_IN_URL");
    }

    @Override
    public int priority() {
        return 10;
    }
}
