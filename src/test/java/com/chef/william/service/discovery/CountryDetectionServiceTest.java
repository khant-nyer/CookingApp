package com.chef.william.service.discovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CountryDetectionServiceTest {

    private final CountryDetectionService service = new CountryDetectionService();

    @Test
    void shouldResolveCountryFromMappedCity() {
        CountryResolutionResult result = service.detectCountryCode("Jakarta");

        assertThat(result.resolved()).isTrue();
        assertThat(result.countryCode()).isEqualTo("ID");
        assertThat(result.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldResolveCountryFromCityCountryInput() {
        CountryResolutionResult result = service.detectCountryCode("Paris, France");

        assertThat(result.resolved()).isTrue();
        assertThat(result.countryCode()).isEqualTo("FR");
        assertThat(result.confidence()).isEqualTo("HIGH");
    }

    @Test
    void shouldFallbackForAmbiguousCity() {
        CountryResolutionResult result = service.detectCountryCode("Springfield");

        assertThat(result.resolved()).isFalse();
        assertThat(result.reason()).isEqualTo("ambiguous_city");
    }
}
