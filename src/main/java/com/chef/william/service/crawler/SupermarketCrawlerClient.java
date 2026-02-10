package com.chef.william.service.crawler;

public interface SupermarketCrawlerClient {
    boolean webpageContainsIngredient(String url, String ingredientName);
}
