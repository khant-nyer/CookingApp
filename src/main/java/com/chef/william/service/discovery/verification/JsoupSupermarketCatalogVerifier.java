package com.chef.william.service.discovery.verification;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Stream;

@Component
public class JsoupSupermarketCatalogVerifier implements CatalogVerificationStrategy {

    @Override
    public CatalogVerificationResult verify(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return CatalogVerificationResult.noMatch("JSOUP", "INVALID_INPUT");
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; CookingAppBot/1.0)")
                    .timeout(7000)
                    .get();

            if (matchFromStructuredProductSignals(doc, ingredientName)) {
                return CatalogVerificationResult.matched("STRUCTURED_PRODUCT_SCRAPE", 0.90);
            }

            if (matchFromGeneralText(doc, ingredientName)) {
                return CatalogVerificationResult.matched("OFFICIAL_WEB_CRAWL", 0.70);
            }

            return CatalogVerificationResult.noMatch("JSOUP", "NO_MATCH_ON_CRAWL");
        } catch (Exception ignored) {
            return CatalogVerificationResult.noMatch("JSOUP", "CRAWL_EXCEPTION");
        }
    }

    @Override
    public int priority() {
        return 20;
    }

    private boolean matchFromStructuredProductSignals(Document doc, String ingredientName) {
        String ingredient = normalize(ingredientName);
        StringBuilder signals = new StringBuilder();

        doc.select("script[type=application/ld+json]").forEach(node -> signals.append(' ').append(node.data()));
        doc.select("[itemtype*=Product],[data-product-id],[class*=product],[class*=item-name]")
                .forEach(node -> signals.append(' ').append(node.text()));

        String normalizedSignals = normalize(signals.toString());
        return containsAllTokens(normalizedSignals, ingredientName) || normalizedSignals.contains(ingredient);
    }

    private boolean matchFromGeneralText(Document doc, String ingredientName) {
        return containsAllTokens(normalize(doc.text()), ingredientName);
    }

    private boolean containsAllTokens(String haystack, String ingredientName) {
        return Stream.of(ingredientName.trim().split("\\s+"))
                .map(this::normalize)
                .filter(token -> !token.isBlank())
                .allMatch(haystack::contains);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
    }
}
