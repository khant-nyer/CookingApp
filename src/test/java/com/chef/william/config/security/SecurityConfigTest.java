package com.chef.william.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityConfigTest {

    @Test
    void jwtDecoderShouldBeCreatedFromCognitoProperties() {
        SecurityConfig securityConfig = new SecurityConfig();

        CognitoProperties properties = new CognitoProperties();
        properties.setRegion("ap-southeast-1");
        properties.setUserPoolId("ap-southeast-1_ABC123");
        properties.setAppClientId("2v36scmicr5rqoqio57g4hnqcv");

        JwtDecoder decoder = securityConfig.jwtDecoder(properties);

        assertNotNull(decoder);
    }
}
