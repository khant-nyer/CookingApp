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

    @Value("${app.discovery.max-metadata-checks:10}")
    private int maxMetadataChecks;

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
            return List.of();
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

                String officialWebsite = supermarket.getOfficialOnlineWebpage();
                if (officialWebsite == null || officialWebsite.isBlank()) {
                    officialWebsite = resolveOfficialWebsite(browser, supermarket.getName(), city, supermarket.getCountry());
                }
                if (officialWebsite == null || officialWebsite.isBlank()) {
                    continue;
                }

                IngredientSignal signal = inspectIngredientSignal(
                        browser,
                        supermarket.getName(),
                        ingredientName,
                        city,
                        officialWebsite
                );
                if (signal.matched()) {
                    matched.add(SupermarketDTO.builder()
                            .name(supermarket.getName())
                            .officialOnlineWebpage(officialWebsite)
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

    public List<SupermarketDTO> enrichSupermarketsMetadata(List<SupermarketDTO> supermarkets,
                                                            String ingredientName,
                                                            String city) {
        if (supermarkets == null || supermarkets.isEmpty()) {
            return List.of();
        }
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            List<SupermarketDTO> enriched = new ArrayList<>();
            int checks = 0;

            for (SupermarketDTO supermarket : supermarkets) {
                if (supermarket == null) {
                    continue;
                }

                String website = supermarket.getOfficialOnlineWebpage();
                String priceRange = supermarket.getMatchedIngredientPriceRange();
                String address = supermarket.getAddress();

                if (checks < maxMetadataChecks && website == null) {
                    checks++;
                    website = resolveOfficialWebsite(browser, supermarket.getName(), city, supermarket.getCountry());
                }

                enriched.add(SupermarketDTO.builder()
                        .name(supermarket.getName())
                        .officialOnlineWebpage(website)
                        .matchedIngredientPriceRange(priceRange)
                        .city(supermarket.getCity())
                        .country(supermarket.getCountry())
                        .address(address == null || address.isBlank() ? city : address)
                        .latitude(supermarket.getLatitude())
                        .longitude(supermarket.getLongitude())
                        .source(supermarket.getSource())
                        .build());
            }

            browser.close();
            return enriched;
        } catch (Exception e) {
            log.warn("Metadata enrichment failed for ingredient {} in city {}: {}", ingredientName, city, e.getMessage());
            return supermarkets;
        }
    }

    private IngredientSignal inspectIngredientSignal(Browser browser,
                                                     String supermarketName,
                                                     String ingredientName,
                                                     String city,
                                                     String officialWebsite) {
        String domain = extractHost(officialWebsite);
        String query = (domain == null)
                ? supermarketName + " " + city + " " + ingredientName
                : "site:" + domain + " " + ingredientName;
        String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        Page page = browser.newPage();
        try {
            String normalizedIngredient = ingredientName.toLowerCase(Locale.ROOT);
            String homepageText = readHomepageText(browser, officialWebsite);

            if (homepageText != null && !homepageText.isBlank() && homepageText.toLowerCase(Locale.ROOT).contains(normalizedIngredient)) {
                String homepagePrice = extractPriceRange(List.of(homepageText), List.of());
                return new IngredientSignal(true, officialWebsite, homepagePrice);
            }

            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

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

            String priceRange = extractPriceRange(snippets, titles);
            return new IngredientSignal(matched, officialWebsite, priceRange);
        } catch (Exception ex) {
            log.debug("Ingredient signal check failed for supermarket {} and ingredient {}: {}", supermarketName, ingredientName, ex.getMessage());
            return IngredientSignal.notMatched();
        } finally {
            page.close();
        }
    }

    private String resolveOfficialWebsite(Browser browser, String supermarketName, String city, String country) {
        String query = supermarketName + " official site " + city + " " + (country == null ? "" : country);
        String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        Page page = browser.newPage();
        try {
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            @SuppressWarnings("unchecked")
            List<String> links = (List<String>) page.evaluate("""
                    () => Array.from(document.querySelectorAll('[data-testid="result-title-a"], h2 a'))
                      .map(n => n.href || '')
                      .filter(Boolean)
                      .filter(href => !href.includes('duckduckgo.com'))
                      .slice(0, 5)
                    """);
            return links.isEmpty() ? null : links.getFirst();
        } catch (Exception e) {
            log.debug("Could not resolve official website for {}: {}", supermarketName, e.getMessage());
            return null;
        } finally {
            page.close();
        }
    }

    private String readHomepageText(Browser browser, String officialWebsite) {
        if (officialWebsite == null || officialWebsite.isBlank()) {
            return null;
        }
        Page page = browser.newPage();
        try {
            page.navigate(officialWebsite, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            return (String) page.evaluate("""
                    () => {
                      const bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                      return bodyText.slice(0, 30000);
                    }
                    """);
        } catch (Exception e) {
            log.debug("Could not read homepage text {}: {}", officialWebsite, e.getMessage());
            return null;
        } finally {
            page.close();
        }
    }

    private String extractHost(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (Exception e) {
            return null;
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
