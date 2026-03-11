package com.chef.william.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;


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
        OAuth2TokenValidator<Jwt> accessTokenValidator = CognitoJwtClaimValidators.accessTokenUse();
        OAuth2TokenValidator<Jwt> clientIdValidator = CognitoJwtClaimValidators.clientIdMatches(cognitoProperties.getAppClientId());

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, accessTokenValidator, clientIdValidator));
        return jwtDecoder;
    }

}
