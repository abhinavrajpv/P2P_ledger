
package com.p2pledger.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "myTestSecretKeyForJwtTestingMustBeAtLeast32BytesLong";

    private static final String WRONG_SECRET =
            "anotherTestSecretKeyForJwtTestingMustBeAtLeast32Bytes";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(1L, "USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUserId() {
        String token = jwtService.generateToken(1L, "USER");

        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void testExtractRole() {
        String token = jwtService.generateToken(1L, "ADMIN");

        String role = jwtService.extractRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = jwtService.generateToken(1L, "USER");

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);

        String token = jwtService.generateToken(1L, "USER");

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.value"));
    }

    @Test
    void testValidateToken_EmptyToken() {
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void testValidateToken_NullToken() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void testValidateToken_WrongSignature() {
        String token = jwtService.generateToken(1L, "USER");

        JwtService anotherJwtService = new JwtService();
        ReflectionTestUtils.setField(
                anotherJwtService, "jwtSecret", WRONG_SECRET);
        ReflectionTestUtils.setField(
                anotherJwtService, "jwtExpirationMs", 3600000L);

        assertFalse(anotherJwtService.validateToken(token));
    }

    @Test
    void testExtractUserId_InvalidToken() {
        assertThrows(
                Exception.class,
                () -> jwtService.extractUserId("invalid.token.value")
        );
    }

    @Test
    void testExtractRole_InvalidToken() {
        assertThrows(
                Exception.class,
                () -> jwtService.extractRole("invalid.token.value")
        );
    }

    @Test
    void testExtractUserId_NonNumericSubject() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

        String token = Jwts.builder()
                .setSubject("invalid-user-id")
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(
                NumberFormatException.class,
                () -> jwtService.extractUserId(token)
        );
    }
}
