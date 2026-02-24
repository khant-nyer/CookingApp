package com.chef.william.service.discovery.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
public class CompositeSupermarketCatalogVerifier implements SupermarketCatalogVerifier {

    private final List<CatalogVerificationStrategy> strategies;

    @Override
    public CatalogVerificationResult verifyIngredient(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return CatalogVerificationResult.noMatch("CHAIN", "INVALID_INPUT");
        }

        return strategies.stream()
                .sorted(Comparator.comparingInt(CatalogVerificationStrategy::priority))
                .map(strategy -> strategy.verify(url, ingredientName))
                .filter(CatalogVerificationResult::isMatched)
                .findFirst()
                .orElse(CatalogVerificationResult.noMatch("CHAIN", "ALL_STRATEGIES_FAILED"));
    }
}
