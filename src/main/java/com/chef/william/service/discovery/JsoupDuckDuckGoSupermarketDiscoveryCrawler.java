package com.chef.william.service.discovery;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class JsoupDuckDuckGoSupermarketDiscoveryCrawler implements SupermarketDiscoveryCrawler {

    private static final List<String> QUERY_TEMPLATES = List.of(
            "online grocery delivery %s %s",
            "supermarket online shopping %s %s",
            "grocery delivery service %s %s"
    );

    private static final Set<String> MARKET_KEYWORDS = Set.of(
            "supermarket", "grocery", "market", "hypermarket", "fresh", "mart"
    );

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "duckduckgo.com", "wikipedia.org", "facebook.com", "instagram.com", "youtube.com"
    );

    @Override
    public List<DiscoveredSupermarket> discover(String city, String countryCode) {
        Map<String, DiscoveredSupermarket> byHost = new LinkedHashMap<>();

        for (String template : QUERY_TEMPLATES) {
            String query = template.formatted(city, countryCode);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8);

            List<DiscoveredSupermarket> discovered = crawlSearchResults(searchUrl);
            for (DiscoveredSupermarket market : discovered) {
                String host = normalizeHost(market.homepage());
                if (host == null || byHost.containsKey(host)) {
                    continue;
                }
                byHost.put(host, market);
                if (byHost.size() >= 8) {
                    log.info("Discovery hit result cap for city='{}' country='{}'", city, countryCode);
                    return new ArrayList<>(byHost.values());
                }
            }
        }

        log.info("Discovery completed city='{}' country='{}' candidates={}", city, countryCode, byHost.size());
        return new ArrayList<>(byHost.values());
    }

    private List<DiscoveredSupermarket> crawlSearchResults(String searchUrl) {
        List<DiscoveredSupermarket> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (compatible; CookingAppDiscoveryBot/1.0)")
                    .timeout(10000)
                    .get();

            for (Element link : doc.select("a.result__a")) {
                String href = link.absUrl("href");
                String text = safeLower(link.text());

                if (!isCandidate(href, text)) {
                    continue;
                }

                String name = deriveName(link.text(), href);
                results.add(new DiscoveredSupermarket(name, href, "duckduckgo_html", "LOW"));
            }
        } catch (Exception e) {
            log.warn("Discovery crawl failed url='{}' error='{}'", searchUrl, e.getMessage());
            return List.of();
        }

        return deduplicateByUrl(results);
    }

    private boolean isCandidate(String href, String title) {
        if (href == null || href.isBlank()) {
            return false;
        }

        String host = normalizeHost(href);
        if (host == null) {
            return false;
        }

        for (String blocked : BLOCKED_DOMAINS) {
            if (host.endsWith(blocked)) {
                return false;
            }
        }

        if (!href.startsWith("http://") && !href.startsWith("https://")) {
            return false;
        }

        for (String keyword : MARKET_KEYWORDS) {
            if (title.contains(keyword) || host.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private List<DiscoveredSupermarket> deduplicateByUrl(List<DiscoveredSupermarket> source) {
        Set<String> seen = new LinkedHashSet<>();
        List<DiscoveredSupermarket> unique = new ArrayList<>();
        for (DiscoveredSupermarket market : source) {
            String normalizedUrl = market.homepage().toLowerCase(Locale.ROOT);
            if (seen.add(normalizedUrl)) {
                unique.add(market);
            }
        }
        return unique;
    }

    private String deriveName(String title, String href) {
        if (title != null && !title.isBlank()) {
            return title.split("-|\\|")[0].trim();
        }

        String host = normalizeHost(href);
        if (host == null) {
            return "Unknown supermarket";
        }

        return host.replace("www.", "");
    }

    private String normalizeHost(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
