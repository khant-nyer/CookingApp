package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.SupermarketDiscoveryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSupermarketMatchingServiceTest {

    private final DefaultSupermarketMatchingService service = new DefaultSupermarketMatchingService();

    @Test
    void shouldFilterOnlyStrongMatchesAndSortByConfidence() {
        List<SupermarketDiscoveryResult> results = service.matchAndRank("milk", List.of(
                new SupermarketInspectionResult("Store B", "https://b.example", "https://b.example/search?q=milk", true, "MEDIUM", true, 3, 1),
                new SupermarketInspectionResult("Store A", "https://a.example", "https://a.example/search?q=milk", true, "HIGH", true, 5, 4),
                new SupermarketInspectionResult("Store C", "https://c.example", "https://c.example/search?q=milk", false, "HIGH", true, 5, 4),
                new SupermarketInspectionResult("Store D", "https://d.example", "https://d.example/search?q=milk", true, "HIGH", false, 5, 4),
                new SupermarketInspectionResult("Store E", "https://e.example", "https://e.example/s?q=other", true, "MEDIUM", true, 2, 1)
        ));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("Store A");
        assertThat(results.get(1).name()).isEqualTo("Store B");
        assertThat(results).allMatch(SupermarketDiscoveryResult::available);
    }
}
