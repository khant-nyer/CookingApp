package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.SupermarketDiscoveryResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class DefaultSupermarketMatchingService implements SupermarketMatchingService {

    @Override
    public List<SupermarketDiscoveryResult> matchAndRank(String ingredient, List<SupermarketInspectionResult> inspections) {
        if (inspections == null || inspections.isEmpty()) {
            return List.of();
        }

        String normalizedIngredient = ingredient == null ? "" : ingredient.toLowerCase(Locale.ROOT).trim();

        return inspections.stream()
                .filter(SupermarketInspectionResult::inspected)
                .filter(result -> isStrongMatch(result, normalizedIngredient))
                .map(result -> new SupermarketDiscoveryResult(
                        result.supermarketName(),
                        result.homepage(),
                        result.ingredientSearchUrl(),
                        true,
                        result.confidence()
                ))
                .sorted(Comparator
                        .comparingInt((SupermarketDiscoveryResult result) -> confidenceRank(result.confidence()))
                        .reversed()
                        .thenComparing(result -> safeLower(result.name())))
                .toList();
    }

    private boolean isStrongMatch(SupermarketInspectionResult result, String ingredient) {
        if (!result.available()) {
            return false;
        }

        boolean ingredientInUrl = result.ingredientSearchUrl() != null
                && !ingredient.isBlank()
                && result.ingredientSearchUrl().toLowerCase(Locale.ROOT).contains(ingredient);

        return result.ingredientMentions() > 0 && result.evidenceScore() >= 2 && (ingredientInUrl || result.evidenceScore() >= 4);
    }

    private int confidenceRank(String confidence) {
        if (confidence == null) {
            return 0;
        }

        return switch (confidence.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
