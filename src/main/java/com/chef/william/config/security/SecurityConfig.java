package com.chef.william.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(CognitoProperties cognitoProperties) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(cognitoProperties.issuerUri());

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(cognitoProperties.issuerUri());
        OAuth2TokenValidator<Jwt> accessTokenValidator = tokenUseValidator();
        OAuth2TokenValidator<Jwt> clientIdValidator = clientIdValidator(cognitoProperties.getAppClientId());

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, accessTokenValidator, clientIdValidator));
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> tokenUseValidator() {
        return jwt -> {
            String tokenUse = jwt.getClaimAsString("token_use");
            if ("access".equals(tokenUse)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Token must be an access token", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    private OAuth2TokenValidator<Jwt> clientIdValidator(String appClientId) {
        return jwt -> {
            String clientId = jwt.getClaimAsString("client_id");
            if (appClientId.equals(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            List<String> audience = jwt.getAudience();
            if (audience != null && audience.contains(appClientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "Token client does not match configured app client", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
