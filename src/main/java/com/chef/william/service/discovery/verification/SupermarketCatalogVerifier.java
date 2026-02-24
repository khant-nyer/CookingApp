package com.chef.william.service.discovery.verification;

public interface SupermarketCatalogVerifier {
    CatalogVerificationResult verifyIngredient(String url, String ingredientName);
}
