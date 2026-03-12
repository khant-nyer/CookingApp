package com.chef.william.config.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.cognito")
public class CognitoProperties {

    @NotBlank
    private String region;

    @NotBlank
    private String userPoolId;

    @NotBlank
    private String appClientId;

    /**
     * Optional comma-separated list of additional app client IDs accepted for JWT validation.
     */
    private String appClientIds;

    /**
     * Optional explicit JWKS URI. When absent, it is derived from region and userPoolId.
     */
    private String jwkSetUri;

    private String appClientSecret;

    public String issuerUri() {
        return "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
    }

    public String resolvedJwkSetUri() {
        if (jwkSetUri != null && !jwkSetUri.trim().isBlank()) {
            return jwkSetUri.trim();
        }
        return issuerUri() + "/.well-known/jwks.json";
    }

    public boolean hasAppClientSecret() {
        return appClientSecret != null && !appClientSecret.isBlank();
    }

    public Set<String> getAllowedAppClientIds() {
        Set<String> allowed = new LinkedHashSet<>();
        if (appClientId != null && !appClientId.trim().isBlank()) {
            allowed.add(appClientId.trim());
        }
        if (appClientIds != null && !appClientIds.isBlank()) {
            Arrays.stream(appClientIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(allowed::add);
        }
        return allowed;
    }
}
