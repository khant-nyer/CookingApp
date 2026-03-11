package com.chef.william.config.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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

    public String issuerUri() {
        return "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
    }
}
