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

                if (hasIngredientSignal(browser, supermarket.getName(), ingredientName, city)) {
                    matched.add(supermarket);
                }
            }

            browser.close();
            return matched;
        } catch (Exception e) {
            log.warn("Ingredient filtering fallback failed for ingredient {} in city {}: {}", ingredientName, city, e.getMessage());
            return List.of();
        }
    }

    private boolean hasIngredientSignal(Browser browser, String supermarketName, String ingredientName, String city) {
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

            return snippets.stream().anyMatch(text -> containsTokens(text, normalizedIngredient, normalizedSupermarket))
                    || titles.stream().anyMatch(text -> containsTokens(text, normalizedIngredient, normalizedSupermarket));
        } catch (Exception ex) {
            log.debug("Ingredient signal check failed for supermarket {} and ingredient {}: {}", supermarketName, ingredientName, ex.getMessage());
            return false;
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
}
