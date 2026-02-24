package com.chef.william.service.discovery.verification;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class RetailerDomainCatalogVerificationStrategy implements CatalogVerificationStrategy {

    private static final Map<String, String> RETAILER_SEARCH_PATHS = new LinkedHashMap<>();

    static {
        RETAILER_SEARCH_PATHS.put("tesco.com", "/groceries/en-GB/search?query={ingredient}");
        RETAILER_SEARCH_PATHS.put("sainsburys.co.uk", "/gol-ui/SearchResults/{ingredient}");
        RETAILER_SEARCH_PATHS.put("groceries.asda.com", "/search/{ingredient}");
        RETAILER_SEARCH_PATHS.put("bigc.co.th", "/search?q={ingredient}");
        RETAILER_SEARCH_PATHS.put("lotuss.com", "/th/search/{ingredient}");
        RETAILER_SEARCH_PATHS.put("tops.co.th", "/en/search?query={ingredient}");
    }

    @Override
    public CatalogVerificationResult verify(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return CatalogVerificationResult.noMatch("RETAILER_DOMAIN_PROBE", "INVALID_INPUT");
        }

        String host = host(url);
        if (host.isBlank()) {
            return CatalogVerificationResult.noMatch("RETAILER_DOMAIN_PROBE", "INVALID_HOST");
        }

        String template = matchTemplate(host);
        if (template.isBlank()) {
            return CatalogVerificationResult.noMatch("RETAILER_DOMAIN_PROBE", "NO_RETAILER_TEMPLATE");
        }

        try {
            String encodedIngredient = URLEncoder.encode(ingredientName.trim(), StandardCharsets.UTF_8);
            String probeUrl = "https://" + host + template.replace("{ingredient}", encodedIngredient);
            Document doc = Jsoup.connect(probeUrl)
                    .userAgent("Mozilla/5.0 (compatible; CookingAppBot/1.0)")
                    .timeout(7000)
                    .get();

            String normalizedText = doc.text().toLowerCase(Locale.ROOT);
            if (normalizedText.contains(ingredientName.trim().toLowerCase(Locale.ROOT))) {
                return CatalogVerificationResult.matched("RETAILER_TEMPLATE_PROBE", 0.80);
            }
            return CatalogVerificationResult.noMatch("RETAILER_DOMAIN_PROBE", "PROBE_NO_INGREDIENT_MATCH");
        } catch (Exception ignored) {
            return CatalogVerificationResult.noMatch("RETAILER_DOMAIN_PROBE", "PROBE_EXCEPTION");
        }
    }

    @Override
    public int priority() {
        return 15;
    }

    private String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return "";
        }
    }

    private String matchTemplate(String host) {
        return RETAILER_SEARCH_PATHS.entrySet().stream()
                .filter(e -> host.equals(e.getKey()) || host.endsWith("." + e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }
}
