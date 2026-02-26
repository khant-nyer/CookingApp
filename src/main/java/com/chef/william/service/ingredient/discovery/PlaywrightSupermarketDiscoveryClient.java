package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PlaywrightSupermarketDiscoveryClient {

    @Value("${app.discovery.max-supermarkets:10}")
    private int maxSupermarkets;

    @Value("${app.discovery.max-ingredient-checks:5}")
    private int maxIngredientChecks;

    public List<SupermarketDTO> discoverBySearch(String city, String country) {
        String query = "popular supermarkets in " + city;
        String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            @SuppressWarnings("unchecked")
            List<String> titles = (List<String>) page.evaluate("""
                    () => Array.from(document.querySelectorAll('[data-testid="result-title-a"], h2 a'))
                      .map(n => (n.textContent || '').trim())
                      .filter(Boolean)
                      .slice(0, 20)
                    """);

            Map<String, SupermarketDTO> unique = new LinkedHashMap<>();
            for (String title : titles) {
                String normalized = title.replaceAll("\\s+-\\s+.*$", "").trim();
                if (normalized.isBlank()) {
                    continue;
                }
                String key = normalized.toLowerCase();
                if (unique.containsKey(key)) {
                    continue;
                }

                unique.put(key, SupermarketDTO.builder()
                        .name(normalized)
                        .officialOnlineWebpage(null)
                        .matchedIngredientPriceRange(null)
                        .city(city)
                        .country(country)
                        .source("PLAYWRIGHT_FALLBACK")
                        .build());

                if (unique.size() >= maxSupermarkets) {
                    break;
                }
            }
            browser.close();
            return new ArrayList<>(unique.values());
        } catch (Exception e) {
            log.warn("Playwright fallback failed for city {}: {}", city, e.getMessage());
            return List.of();
        }
    }

    public List<SupermarketDTO> filterSupermarketsByIngredient(List<SupermarketDTO> supermarkets,
                                                                String ingredientName,
                                                                String city) {
        if (supermarkets == null || supermarkets.isEmpty() || ingredientName == null || ingredientName.isBlank()) {
            return supermarkets == null ? List.of() : supermarkets;
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            List<SupermarketDTO> matched = new ArrayList<>();
            int checks = 0;

            for (SupermarketDTO supermarket : supermarkets) {
                if (checks >= maxIngredientChecks) {
                    break;
                }
                checks++;

                IngredientSignal signal = inspectIngredientSignal(browser, supermarket.getName(), ingredientName, city);
                if (signal.matched()) {
                    matched.add(SupermarketDTO.builder()
                            .name(supermarket.getName())
                            .officialOnlineWebpage(signal.officialWebpage() != null ? signal.officialWebpage() : supermarket.getOfficialOnlineWebpage())
                            .matchedIngredientPriceRange(signal.priceRange())
                            .city(supermarket.getCity())
                            .country(supermarket.getCountry())
                            .address(supermarket.getAddress())
                            .latitude(supermarket.getLatitude())
                            .longitude(supermarket.getLongitude())
                            .source(supermarket.getSource())
                            .build());
                }
            }

            browser.close();
            return matched;
        } catch (Exception e) {
            log.warn("Ingredient filtering fallback failed for ingredient {} in city {}: {}", ingredientName, city, e.getMessage());
            return List.of();
        }
    }

    private IngredientSignal inspectIngredientSignal(Browser browser, String supermarketName, String ingredientName, String city) {
        String query = supermarketName + " " + city + " " + ingredientName;
        String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        Page page = browser.newPage();
        try {
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            String normalizedIngredient = ingredientName.toLowerCase(Locale.ROOT);
            String normalizedSupermarket = supermarketName.toLowerCase(Locale.ROOT);

            @SuppressWarnings("unchecked")
            List<String> snippets = (List<String>) page.evaluate("""
                    () => Array.from(document.querySelectorAll('[data-result="snippet"], .result__snippet, [data-testid="result-snippet"]'))
                      .map(n => (n.textContent || '').trim())
                      .filter(Boolean)
                      .slice(0, 10)
                    """);

            @SuppressWarnings("unchecked")
            List<String> titles = (List<String>) page.evaluate("""
                    () => Array.from(document.querySelectorAll('[data-testid="result-title-a"], h2 a'))
                      .map(n => (n.textContent || '').trim())
                      .filter(Boolean)
                      .slice(0, 10)
                    """);

            @SuppressWarnings("unchecked")
            List<String> links = (List<String>) page.evaluate("""
                    () => Array.from(document.querySelectorAll('[data-testid="result-title-a"], h2 a'))
                      .map(n => n.href || '')
                      .filter(Boolean)
                      .slice(0, 5)
                    """);

            boolean matched = snippets.stream().anyMatch(text -> containsTokens(text, normalizedIngredient, normalizedSupermarket))
                    || titles.stream().anyMatch(text -> containsTokens(text, normalizedIngredient, normalizedSupermarket));
            if (!matched) {
                return IngredientSignal.notMatched();
            }

            String priceRange = extractPriceRange(snippets, titles);
            String website = links.isEmpty() ? null : links.getFirst();

            return new IngredientSignal(true, website, priceRange);
        } catch (Exception ex) {
            log.debug("Ingredient signal check failed for supermarket {} and ingredient {}: {}", supermarketName, ingredientName, ex.getMessage());
            return IngredientSignal.notMatched();
        } finally {
            page.close();
        }
    }

    private boolean containsTokens(String text, String ingredient, String supermarket) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains(ingredient) && normalized.contains(supermarket);
    }

    private String extractPriceRange(List<String> snippets, List<String> titles) {
        List<String> corpus = new ArrayList<>();
        corpus.addAll(snippets);
        corpus.addAll(titles);

        Pattern pricePattern = Pattern.compile("(?i)(?:฿|thb|php|₱|\$|usd|eur|€|£)?\\s*(\\d{1,4}(?:[.,]\\d{1,2})?)");
        Double min = null;
        Double max = null;

        for (String text : corpus) {
            Matcher matcher = pricePattern.matcher(text);
            while (matcher.find()) {
                try {
                    double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
                    if (value <= 0 || value > 10000) {
                        continue;
                    }
                    min = (min == null || value < min) ? value : min;
                    max = (max == null || value > max) ? value : max;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (min == null || max == null) {
            return null;
        }
        if (Double.compare(min, max) == 0) {
            return String.format("%.2f", min);
        }
        return String.format("%.2f-%.2f", min, max);
    }

    private record IngredientSignal(boolean matched, String officialWebpage, String priceRange) {
        private static IngredientSignal notMatched() {
            return new IngredientSignal(false, null, null);
        }
    }
}
