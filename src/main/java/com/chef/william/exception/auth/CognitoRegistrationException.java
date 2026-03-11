package com.chef.william.exception.auth;

public class CognitoRegistrationException extends RuntimeException {
    public CognitoRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
