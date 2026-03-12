package com.chef.william.config.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CognitoPropertiesTest {

    @Test
    void resolvedJwkSetUriShouldDefaultFromIssuer() {
        CognitoProperties properties = new CognitoProperties();
        properties.setRegion("ap-southeast-2");
        properties.setUserPoolId("ap-southeast-2_example");

        assertEquals(
                "https://cognito-idp.ap-southeast-2.amazonaws.com/ap-southeast-2_example/.well-known/jwks.json",
                properties.resolvedJwkSetUri()
        );
    }

    @Test
    void resolvedJwkSetUriShouldUseExplicitValueWhenConfigured() {
        CognitoProperties properties = new CognitoProperties();
        properties.setRegion("ap-southeast-2");
        properties.setUserPoolId("ap-southeast-2_example");
        properties.setJwkSetUri("  https://custom.example/jwks.json ");

        assertEquals("https://custom.example/jwks.json", properties.resolvedJwkSetUri());
    }
}
