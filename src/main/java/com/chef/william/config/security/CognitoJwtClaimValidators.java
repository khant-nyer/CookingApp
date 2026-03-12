package com.chef.william.config.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class CognitoJwtClaimValidators {

    private CognitoJwtClaimValidators() {
    }

    public static OAuth2TokenValidator<Jwt> accessTokenUse() {
        return jwt -> {
            String tokenUse = jwt.getClaimAsString("token_use");
            if ("access".equals(tokenUse)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Token must be an access token", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    public static OAuth2TokenValidator<Jwt> clientIdMatches(String appClientId) {
        return clientIdMatches(List.of(appClientId));
    }

    public static OAuth2TokenValidator<Jwt> clientIdMatches(Collection<String> appClientIds) {
        return jwt -> {
            Set<String> expectedClientIds = appClientIds == null
                    ? Set.of()
                    : appClientIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(id -> !id.isBlank())
                    .collect(Collectors.toSet());

            if (expectedClientIds.isEmpty()) {
                OAuth2Error error = new OAuth2Error("invalid_token", "Configured app client id is blank", null);
                return OAuth2TokenValidatorResult.failure(error);
            }

            String clientId = jwt.getClaimAsString("client_id");
            if (clientId != null && expectedClientIds.contains(clientId.trim())) {
                return OAuth2TokenValidatorResult.success();
            }

            List<String> audience = jwt.getAudience();
            if (audience != null && audience.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .anyMatch(expectedClientIds::contains)) {
                return OAuth2TokenValidatorResult.success();
            }

            String authorizedParty = jwt.getClaimAsString("azp");
            if (authorizedParty != null && expectedClientIds.contains(authorizedParty.trim())) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "Token client does not match configured app client", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
