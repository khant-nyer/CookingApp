package com.chef.william.exception;

import com.chef.william.exception.exceptionResponse.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationShouldMapFoodNameConstraintMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry for key food_name_unique"
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Food name already exists. Please use a unique name.", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolationShouldKeepGenericMessageWhenConstraintUnknown() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Foreign key violation");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Data conflict detected. Please verify unique fields and constraints.", response.getBody().getMessage());
    }
}
