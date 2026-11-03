package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class LoginAttemptsTest {

    private LoginAttempts createLoginAttempt() {
        return LoginAttempts.builder()
                .id(1L)
                .userId(10L)
                .identifier("user@example.com")
                .attemptedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .successful(true)
                .source("192.168.1.1")
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        LoginAttempts attempt = new LoginAttempts();

        assertNull(attempt.getId());
        assertNull(attempt.getUserId());
        assertNull(attempt.getIdentifier());
        assertNull(attempt.getAttemptedAt());
        assertFalse(attempt.isSuccessful());
        assertNull(attempt.getSource());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime attemptedAt =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        LoginAttempts attempt = new LoginAttempts(
                1L,
                10L,
                "user@example.com",
                attemptedAt,
                true,
                "192.168.1.1"
        );

        assertEquals(1L, attempt.getId());
        assertEquals(10L, attempt.getUserId());
        assertEquals("user@example.com", attempt.getIdentifier());
        assertEquals(attemptedAt, attempt.getAttemptedAt());
        assertTrue(attempt.isSuccessful());
        assertEquals("192.168.1.1", attempt.getSource());
    }

    @Test
    void gettersAndSettersTest() {
        LoginAttempts attempt = new LoginAttempts();

        LocalDateTime attemptedAt =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        attempt.setId(1L);
        attempt.setUserId(10L);
        attempt.setIdentifier("user@example.com");
        attempt.setAttemptedAt(attemptedAt);
        attempt.setSuccessful(true);
        attempt.setSource("192.168.1.1");

        assertEquals(1L, attempt.getId());
        assertEquals(10L, attempt.getUserId());
        assertEquals("user@example.com", attempt.getIdentifier());
        assertEquals(attemptedAt, attempt.getAttemptedAt());
        assertTrue(attempt.isSuccessful());
        assertEquals("192.168.1.1", attempt.getSource());
    }

    @Test
    void successfulSetterFalseTest() {
        LoginAttempts attempt = createLoginAttempt();

        attempt.setSuccessful(false);

        assertFalse(attempt.isSuccessful());
    }

    @Test
    void builderTest() {
        LoginAttempts attempt = createLoginAttempt();

        assertEquals(1L, attempt.getId());
        assertEquals(10L, attempt.getUserId());
        assertEquals("user@example.com", attempt.getIdentifier());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 10, 0),
                attempt.getAttemptedAt()
        );
        assertTrue(attempt.isSuccessful());
        assertEquals("192.168.1.1", attempt.getSource());
    }

    @Test
    void builderWithNullableFieldsTest() {
        LoginAttempts attempt = LoginAttempts.builder()
                .id(2L)
                .userId(null)
                .identifier("unknown@example.com")
                .attemptedAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .successful(false)
                .source(null)
                .build();

        assertNull(attempt.getUserId());
        assertNull(attempt.getSource());
        assertFalse(attempt.isSuccessful());
    }

    @Test
    void equalsSameObjectTest() {
        LoginAttempts attempt = createLoginAttempt();

        assertEquals(attempt, attempt);
    }

    @Test
    void equalsEqualObjectsTest() {
        LoginAttempts attempt1 = createLoginAttempt();
        LoginAttempts attempt2 = createLoginAttempt();

        assertEquals(attempt1, attempt2);
    }

    @Test
    void equalsDifferentObjectsTest() {
        LoginAttempts attempt1 = createLoginAttempt();

        LoginAttempts attempt2 = LoginAttempts.builder()
                .id(2L)
                .userId(20L)
                .identifier("another@example.com")
                .attemptedAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .successful(false)
                .source("192.168.1.2")
                .build();

        assertNotEquals(attempt1, attempt2);
    }

    @Test
    void equalsNullTest() {
        LoginAttempts attempt = createLoginAttempt();

        assertNotEquals(null, attempt);
    }

    @Test
    void canEqualTest() {
        LoginAttempts attempt = createLoginAttempt();

        assertTrue(attempt.canEqual(createLoginAttempt()));
        assertFalse(attempt.canEqual("not a login attempt"));
        assertFalse(attempt.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        LoginAttempts attempt = createLoginAttempt();

        LoginAttempts other = new LoginAttempts() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(attempt, other);
    }

    @Test
    void hashCodeTest() {
        LoginAttempts attempt1 = createLoginAttempt();
        LoginAttempts attempt2 = createLoginAttempt();

        assertEquals(attempt1.hashCode(), attempt2.hashCode());
    }

    @Test
    void toStringTest() {
        LoginAttempts attempt = createLoginAttempt();

        String result = attempt.toString();

        assertTrue(result.contains("user@example.com"));
        assertTrue(result.contains("successful=true"));
        assertTrue(result.contains("192.168.1.1"));
    }

    @Test
    void equalsDifferentSuccessfulValueTest() {
        LoginAttempts attempt1 = createLoginAttempt();
        LoginAttempts attempt2 = createLoginAttempt();

        attempt2.setSuccessful(false);

        assertNotEquals(attempt1, attempt2);
    }
}