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
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; CookingAppBot/1.0)")
                    .timeout(7000)
                    .get();
            return doc.text().toLowerCase().contains(ingredientName.toLowerCase());
        } catch (Exception ignored) {
            return false;
        }
    }
}
