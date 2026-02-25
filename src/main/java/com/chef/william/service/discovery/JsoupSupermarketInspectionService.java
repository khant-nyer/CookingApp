package com.chef.william.service.discovery;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
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
            Document searchDoc = fetch(searchUrl);

            int mentionsHomepage = countIngredientMentions(homepageDoc.text(), ingredient);
            int mentionsSearch = countIngredientMentions(searchDoc.text(), ingredient);
            int ingredientMentions = mentionsHomepage + mentionsSearch;

            int productSignalScore = scoreProductSignals(searchDoc) + scoreProductSignals(homepageDoc);
            boolean available = ingredientMentions > 0 && productSignalScore >= 2;

            String confidence = classifyConfidence(ingredientMentions, productSignalScore, available);

            return new SupermarketInspectionResult(
                    supermarket.name(),
                    homepage,
                    searchUrl,
                    available,
                    confidence,
                    true,
                    productSignalScore,
                    ingredientMentions
            );
        } catch (Exception e) {
            log.warn("Inspection failed supermarket='{}' url='{}' error='{}'",
                    supermarket.name(), homepage, e.getMessage());
            return new SupermarketInspectionResult(
                    supermarket.name(),
                    homepage,
                    searchUrl,
                    false,
                    "LOW",
                    false,
                    0,
                    0
            );
        }
    }

    private SupermarketInspectionResult buildFailed(DiscoveredSupermarket supermarket, String confidence) {
        String name = supermarket == null ? "unknown" : supermarket.name();
        String homepage = supermarket == null ? null : supermarket.homepage();
        return new SupermarketInspectionResult(name, homepage, homepage, false, confidence, false, 0, 0);
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; CookingAppInspectionBot/1.0)")
                .timeout(10000)
                .get();
    }

    private int countIngredientMentions(String text, String ingredient) {
        if (text == null || ingredient == null) {
            return 0;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String token = ingredient.toLowerCase(Locale.ROOT);
        int count = 0;
        int idx = 0;
        while ((idx = lowerText.indexOf(token, idx)) >= 0) {
            count++;
            idx += token.length();
        }
        return count;
    }

    private int scoreProductSignals(Document doc) {
        int score = 0;
        score += countAtMost(doc.select("[class*=product], [class*=item], [class*=card], [data-testid*=product]"), 3);
        score += countAtMost(doc.select(".price, [class*=price], [data-price]"), 2);
        score += countAtMost(doc.select(":containsOwn(add to cart), :containsOwn(in stock), :containsOwn(buy now)"), 2);
        return score;
    }

    private int countAtMost(Elements elements, int cap) {
        return Math.min(elements.size(), cap);
    }

    private String classifyConfidence(int mentions, int productScore, boolean available) {
        if (!available) {
            return "LOW";
        }
        if (mentions >= 3 && productScore >= 4) {
            return "HIGH";
        }
        return "MEDIUM";
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
