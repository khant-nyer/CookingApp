package com.chef.william.service.discovery;

public record SupermarketInspectionResult(
        String supermarketName,
        String homepage,
        String ingredientSearchUrl,
        boolean available,
        String confidence,
        boolean inspected
) {
}
