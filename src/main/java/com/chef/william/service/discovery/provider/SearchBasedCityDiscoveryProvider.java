package com.chef.william.service.discovery.provider;

import com.chef.william.config.CityDiscoveryProperties;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SearchBasedCityDiscoveryProvider implements CityDiscoverySourceProvider {

    private final CityDiscoveryProperties properties;

    @Override
    public List<CityDiscoveryCandidate> discoverSupermarkets(String city) {
        if (!properties.isSearchProviderEnabled() || city == null || city.isBlank()) {
            return List.of();
        }

        try {
            String q = URLEncoder.encode("popular supermarket " + city, StandardCharsets.UTF_8);
            String url = properties.getSearchProviderBaseUrl() + "?q=" + q;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; CookingAppBot/1.0)")
                    .timeout(7000)
                    .get();

            List<CityDiscoveryCandidate> result = new ArrayList<>();
            for (Element link : doc.select("a.result__a, a[href]")) {
                String href = link.absUrl("href");
                String name = link.text().trim();
                if (name.isBlank() || href.isBlank()) {
                    continue;
                }

                String host = safeHost(href);
                if (host.isBlank() || !host.contains(".")) {
                    continue;
                }
                if (host.contains("wikipedia.org") || host.contains("facebook.com") || host.contains("tripadvisor")
                        || isSearchEngineHost(host)) {
                    continue;
                }

                result.add(new CityDiscoveryCandidate(name, "https://" + host, 0.45));
                if (result.size() >= Math.max(3, properties.getMaxCandidates())) {
                    break;
                }
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @Override
    public String sourceName() {
        return "search";
    }

    private boolean isSearchEngineHost(String host) {
        return host.contains("duckduckgo.com")
                || host.contains("google.")
                || host.contains("bing.com")
                || host.contains("search.yahoo.com")
                || host.contains("yahoo.com");
    }

    private String safeHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return "";
        }
    }
}
