
package com.p2pledger.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgumentTest() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Invalid input");

        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid input", response.getBody().get("message"));
    }

    @Test
    void handleGenericWithMessageTest() {
        Exception exception = new Exception("Database error");

        ResponseEntity<Map<String, String>> response =
                handler.handleGeneric(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().get("message"));
    }

    @Test
    void handleGenericWithNullMessageTest() {
        Exception exception = new Exception((String) null);

        ResponseEntity<Map<String, String>> response =
                handler.handleGeneric(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(
                "An unexpected error occurred",
                response.getBody().get("message")
        );
    }
}
