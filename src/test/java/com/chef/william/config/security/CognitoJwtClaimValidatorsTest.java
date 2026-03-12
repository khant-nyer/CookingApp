package com.chef.william.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitoJwtClaimValidatorsTest {

    @Test
    void accessTokenValidatorShouldFailForIdToken() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "id", "client_id", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.accessTokenUse().validate(jwt);
        assertTrue(result.hasErrors());
    }

    @Test
    void accessTokenValidatorShouldPassForAccessToken() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.accessTokenUse().validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void clientIdValidatorShouldPassWhenClientIdClaimMatches() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches("client-a").validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void clientIdValidatorShouldPassWhenAudienceContainsClientId() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access"), List.of("client-a"));
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches("client-a").validate(jwt);
        assertFalse(result.hasErrors());
    }



    @Test
    void clientIdValidatorShouldPassWhenAzpMatchesClientId() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "azp", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches("client-a").validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void clientIdValidatorShouldPassWhenConfiguredClientIdHasWhitespace() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches("  client-a  ").validate(jwt);
        assertFalse(result.hasErrors());
    }



    @Test
    void clientIdValidatorShouldPassWhenAnyConfiguredClientIdMatches() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-b"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches(List.of("client-a", "client-b")).validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void clientIdValidatorShouldFailWhenConfiguredClientIdsAreBlank() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-a"), List.of());
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches(List.of("  ", "")).validate(jwt);
        assertTrue(result.hasErrors());
    }

    @Test
    void clientIdValidatorShouldFailWhenNoClaimMatches() {
        Jwt jwt = jwtWithClaims(Map.of("token_use", "access", "client_id", "client-b"), List.of("other"));
        OAuth2TokenValidatorResult result = CognitoJwtClaimValidators.clientIdMatches("client-a").validate(jwt);
        assertTrue(result.hasErrors());
    }

    private Jwt jwtWithClaims(Map<String, Object> claims, List<String> audience) {
        return new Jwt(
                "token",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                buildClaims(claims, audience)
        );
    }

    private Map<String, Object> buildClaims(Map<String, Object> claims, List<String> audience) {
        java.util.Map<String, Object> merged = new java.util.HashMap<>(claims);
        merged.put("aud", audience);
        return merged;
    }
}
