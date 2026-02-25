package com.chef.william.service.discovery;

public record CountryResolutionResult(
        String countryCode,
        String confidence,
        boolean resolved,
        String reason
) {
}
