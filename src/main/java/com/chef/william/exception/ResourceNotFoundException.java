package com.chef.william.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)  // Automatically returns 404 when thrown
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }

    // Optional: Simpler constructor for basic messages (e.g., "Ingredient not found")
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Optional: With cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}