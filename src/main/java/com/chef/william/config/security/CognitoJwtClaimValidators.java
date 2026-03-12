package com.chef.william.config.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Objects;

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
        return jwt -> {
            String expectedClientId = appClientId == null ? null : appClientId.trim();
            if (expectedClientId == null || expectedClientId.isBlank()) {
                OAuth2Error error = new OAuth2Error("invalid_token", "Configured app client id is blank", null);
                return OAuth2TokenValidatorResult.failure(error);
            }

            String clientId = jwt.getClaimAsString("client_id");
            if (expectedClientId.equals(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            List<String> audience = jwt.getAudience();
            if (audience != null && audience.stream().filter(Objects::nonNull).map(String::trim).anyMatch(expectedClientId::equals)) {
                return OAuth2TokenValidatorResult.success();
            }

            String authorizedParty = jwt.getClaimAsString("azp");
            if (expectedClientId.equals(authorizedParty)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "Token client does not match configured app client", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
