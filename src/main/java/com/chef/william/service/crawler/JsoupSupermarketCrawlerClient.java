package com.chef.william.service.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class JsoupSupermarketCrawlerClient implements SupermarketCrawlerClient {

    @Override
    public boolean webpageContainsIngredient(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return false;
        }

        try {
            Document doc = fetch(url);
            return doc.text().toLowerCase().contains(ingredientName.toLowerCase());
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean webpageReachable(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            fetch(url);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; CookingAppBot/1.0)")
                .timeout(7000)
                .get();
    }
}
