package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.LoadState;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PlaywrightSupermarketDiscoveryClient {

    @Value("${app.discovery.max-supermarkets:10}")
    private int maxSupermarkets;

    public List<SupermarketDTO> discoverBySearch(String city, String country) {
        String query = "popular supermarkets in " + city;
        String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(LoadState.DOMCONTENTLOADED));

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
}
