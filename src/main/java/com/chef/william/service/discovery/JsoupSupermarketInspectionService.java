package com.chef.william.service.discovery;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class JsoupSupermarketInspectionService implements SupermarketInspectionService {

    @Override
    public SupermarketInspectionResult inspect(DiscoveredSupermarket supermarket, String ingredient) {
        if (supermarket == null || ingredient == null || ingredient.isBlank()) {
            return buildFailed(supermarket, "LOW");
        }

        String homepage = supermarket.homepage();
        String searchUrl = buildSearchUrl(homepage, ingredient);

        try {
            Document homepageDoc = fetch(homepage);
            boolean homepageContainsIngredient = containsIngredient(homepageDoc.text(), ingredient);

            Document searchDoc = fetch(searchUrl);
            boolean searchContainsIngredient = containsIngredient(searchDoc.text(), ingredient);

            boolean hasSignals = hasProductSignals(searchDoc.text()) || hasProductSignals(homepageDoc.text());
            boolean available = searchContainsIngredient || (homepageContainsIngredient && hasSignals);

            String confidence = available ? "MEDIUM" : "LOW";
            return new SupermarketInspectionResult(
                    supermarket.name(),
                    homepage,
                    searchUrl,
                    available,
                    confidence,
                    true
            );
        } catch (Exception ignored) {
            return new SupermarketInspectionResult(
                    supermarket.name(),
                    homepage,
                    searchUrl,
                    false,
                    "LOW",
                    false
            );
        }
    }

    private SupermarketInspectionResult buildFailed(DiscoveredSupermarket supermarket, String confidence) {
        String name = supermarket == null ? "unknown" : supermarket.name();
        String homepage = supermarket == null ? null : supermarket.homepage();
        return new SupermarketInspectionResult(name, homepage, homepage, false, confidence, false);
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; CookingAppInspectionBot/1.0)")
                .timeout(10000)
                .get();
    }

    private boolean containsIngredient(String text, String ingredient) {
        if (text == null || ingredient == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(ingredient.toLowerCase(Locale.ROOT));
    }

    private boolean hasProductSignals(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return lower.contains("price") || lower.contains("add to cart") || lower.contains("in stock") ||
                lower.contains("product") || lower.contains("buy now");
    }

    private String buildSearchUrl(String homepage, String ingredient) {
        try {
            URI uri = URI.create(homepage);
            String base = uri.getScheme() + "://" + uri.getHost();
            return base + "/search?q=" + URLEncoder.encode(ingredient, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return homepage;
        }
    }
}
