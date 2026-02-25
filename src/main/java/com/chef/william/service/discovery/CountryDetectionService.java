package com.chef.william.service.discovery;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CountryDetectionService {

    private static final Set<String> AMBIGUOUS_CITIES = Set.of(
            "springfield", "san jose", "georgetown", "victoria", "cambridge"
    );

    private static final Map<String, String> CITY_TO_COUNTRY = Map.ofEntries(
            Map.entry("jakarta", "ID"),
            Map.entry("bandung", "ID"),
            Map.entry("surabaya", "ID"),
            Map.entry("kuala lumpur", "MY"),
            Map.entry("singapore", "SG"),
            Map.entry("london", "GB"),
            Map.entry("manchester", "GB"),
            Map.entry("paris", "FR"),
            Map.entry("berlin", "DE"),
            Map.entry("tokyo", "JP"),
            Map.entry("osaka", "JP"),
            Map.entry("new york", "US"),
            Map.entry("los angeles", "US"),
            Map.entry("toronto", "CA"),
            Map.entry("sydney", "AU")
    );

    public CountryResolutionResult detectCountryCode(String rawCity) {
        if (rawCity == null || rawCity.isBlank()) {
            return new CountryResolutionResult(null, "NONE", false, "empty_city");
        }

        String city = rawCity.trim().toLowerCase(Locale.ROOT);

        String fromComma = resolveFromCommaSeparatedCity(city);
        if (fromComma != null) {
            return new CountryResolutionResult(fromComma, "HIGH", true, "city_country_input");
        }

        if (AMBIGUOUS_CITIES.contains(city)) {
            return new CountryResolutionResult(null, "LOW", false, "ambiguous_city");
        }

        String mapped = CITY_TO_COUNTRY.get(city);
        if (mapped != null) {
            return new CountryResolutionResult(mapped, "MEDIUM", true, "city_lookup");
        }

        return new CountryResolutionResult(null, "LOW", false, "unknown_city");
    }

    private String resolveFromCommaSeparatedCity(String city) {
        if (!city.contains(",")) {
            return null;
        }

        String[] parts = city.split(",");
        if (parts.length < 2) {
            return null;
        }

        String countryPart = parts[parts.length - 1].trim();
        if (countryPart.length() == 2) {
            return countryPart.toUpperCase(Locale.ROOT);
        }

        for (String iso : Locale.getISOCountries()) {
            Locale locale = new Locale("", iso);
            if (locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(countryPart)) {
                return iso;
            }
        }

        return null;
    }
}
