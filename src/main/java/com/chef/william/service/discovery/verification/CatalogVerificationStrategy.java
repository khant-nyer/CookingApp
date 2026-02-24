package com.chef.william.service.discovery.verification;

public interface CatalogVerificationStrategy {
    CatalogVerificationResult verify(String url, String ingredientName);

    int priority();
}
