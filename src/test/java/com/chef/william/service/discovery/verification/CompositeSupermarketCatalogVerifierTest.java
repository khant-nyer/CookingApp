package com.chef.william.service.discovery.verification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeSupermarketCatalogVerifierTest {

    @Test
    void shouldReturnFirstMatchedStrategyByPriority() {
        CatalogVerificationStrategy lowPriority = new StubStrategy(50,
                CatalogVerificationResult.matched("LOW_PRIORITY", 0.5));
        CatalogVerificationStrategy highPriority = new StubStrategy(10,
                CatalogVerificationResult.matched("HIGH_PRIORITY", 0.8));

        CompositeSupermarketCatalogVerifier verifier = new CompositeSupermarketCatalogVerifier(
                List.of(lowPriority, highPriority)
        );

        CatalogVerificationResult result = verifier.verifyIngredient("https://example.com/search?q=soy+sauce", "Soy Sauce");

        assertTrue(result.isMatched());
        assertEquals("HIGH_PRIORITY", result.getMatchSource());
        assertEquals(0.8, result.getConfidence());
    }

    @Test
    void shouldReturnNoMatchWhenAllStrategiesFail() {
        CatalogVerificationStrategy first = new StubStrategy(10,
                CatalogVerificationResult.noMatch("A", "NO_MATCH"));
        CatalogVerificationStrategy second = new StubStrategy(20,
                CatalogVerificationResult.noMatch("B", "NO_MATCH"));

        CompositeSupermarketCatalogVerifier verifier = new CompositeSupermarketCatalogVerifier(List.of(first, second));

        CatalogVerificationResult result = verifier.verifyIngredient("https://example.com", "Rice");

        assertFalse(result.isMatched());
        assertEquals("CHAIN", result.getMatchSource());
        assertEquals("ALL_STRATEGIES_FAILED", result.getFailureReason());
    }

    private record StubStrategy(int priority, CatalogVerificationResult result) implements CatalogVerificationStrategy {
        @Override
        public CatalogVerificationResult verify(String url, String ingredientName) {
            return result;
        }
    }
}
