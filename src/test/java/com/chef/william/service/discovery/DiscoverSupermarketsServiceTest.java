package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverSupermarketsServiceTest {

    private final DiscoverSupermarketsService service = new DiscoverSupermarketsService(new CountryDetectionService());

    @Test
    void shouldReturnSuccessWhenCountryDetected() {
        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.meta().phase()).isEqualTo("phase0");
        assertThat(response.data()).isEmpty();
    }

    @Test
    void shouldReturnFallbackWhenCountryNotDetected() {
        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Springfield")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().resolvedCountryCode()).isNull();
        assertThat(response.message()).contains("couldn’t confidently detect the country");
    }
}
