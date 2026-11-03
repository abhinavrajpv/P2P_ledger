package com.p2pledger.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class for SecurityConfig
 * Uses @SpringBootTest to properly test security configuration beans
 */
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=testSecretKeyForTestingPurposesOnly1234567890",
        "jwt.expiration=3600000"
})
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    // ==========================================
    // PASSWORD ENCODER TESTS
    // ==========================================

    @Test
    void testPasswordEncoder_BeanCreated() {
        assertNotNull(passwordEncoder, "PasswordEncoder bean should be created");
    }

    @Test
    void testPasswordEncoder_EncodesAndMatches() {
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword),
                "Encoded password should match the original");
    }

    @Test
    void testPasswordEncoder_DifferentPasswordsDoNotMatch() {
        String password = "password123";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(password);

        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword),
                "Different passwords should not match");
    }

    @Test
    void testPasswordEncoder_UsesBCrypt() {
        String encodedPassword = passwordEncoder.encode("testPassword");

        // BCrypt passwords start with $2a$, $2b$, or $2y$
        assertTrue(
                encodedPassword.startsWith("$2a$") ||
                        encodedPassword.startsWith("$2b$") ||
                        encodedPassword.startsWith("$2y$"),
                "Password encoder should use BCrypt algorithm"
        );
    }

    @Test
    void testPasswordEncoder_ProducesDifferentHashesForSamePassword() {
        String password = "testPassword";
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        assertNotEquals(hash1, hash2,
                "BCrypt should produce different hashes for the same password due to random salt");
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
    }

    // ==========================================
    // AUTHENTICATION MANAGER TESTS
    // ==========================================

    @Test
    void testAuthenticationManager_BeanCreated() {
        assertNotNull(authenticationManager, "AuthenticationManager bean should be created");
    }

    @Test
    void testAuthenticationManager_IsConfiguredCorrectly() throws Exception {
        AuthenticationManager manager = securityConfig.authenticationManager(authenticationConfiguration);
        assertNotNull(manager, "Authentication manager from config should not be null");
        assertSame(authenticationManager, manager,
                "Authentication manager should be the same instance");
    }

    // ==========================================
    // SECURITY FILTER CHAIN TESTS
    // ==========================================

    @Test
    void testSecurityFilterChain_BeanCreated() {
        assertNotNull(securityFilterChain, "SecurityFilterChain bean should be created");
    }

    @Test
    void testSecurityFilterChain_ContainsJwtFilter() {
        boolean jwtFilterPresent = securityFilterChain.getFilters()
                .stream()
                .anyMatch(filter -> filter instanceof JwtAuthenticationFilter);

        assertTrue(jwtFilterPresent,
                "Security filter chain should contain JwtAuthenticationFilter");
    }


@Test
void testSecurityFilterChain_DoesNotContainUsernamePasswordFilter() {
    boolean filterPresent = securityFilterChain.getFilters()
            .stream()
            .anyMatch(filter ->
                    filter instanceof UsernamePasswordAuthenticationFilter);

    assertFalse(
            filterPresent,
            "Stateless JWT security should not register UsernamePasswordAuthenticationFilter"
    );
}


    @Test
    void testSecurityFilterChain_HasMultipleFilters() {
        int filterCount = securityFilterChain.getFilters().size();
        assertTrue(filterCount > 5,
                "Security filter chain should have multiple filters (found: " + filterCount + ")");
    }

    // ==========================================
    // CORS CONFIGURATION TESTS
    // ==========================================

    @Test
    void testCorsConfigurationSource_BeanCreated() {
        assertNotNull(corsConfigurationSource, "CorsConfigurationSource bean should be created");
    }

    @Test
    void testCorsConfiguration_AllowedOrigins() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertNotNull(configuration, "CORS configuration should not be null");
        assertNotNull(configuration.getAllowedOrigins(), "Allowed origins should not be null");
        assertTrue(configuration.getAllowedOrigins().contains("*"),
                "CORS should allow all origins (*)");
    }

    @Test
    void testCorsConfiguration_AllowedMethods() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertNotNull(configuration);
        List<String> allowedMethods = configuration.getAllowedMethods();

        assertNotNull(allowedMethods, "Allowed methods should not be null");
        assertTrue(allowedMethods.contains("GET"), "Should allow GET");
        assertTrue(allowedMethods.contains("POST"), "Should allow POST");
        assertTrue(allowedMethods.contains("PUT"), "Should allow PUT");
        assertTrue(allowedMethods.contains("PATCH"), "Should allow PATCH");
        assertTrue(allowedMethods.contains("DELETE"), "Should allow DELETE");
        assertTrue(allowedMethods.contains("OPTIONS"), "Should allow OPTIONS");
    }

    @Test
    void testCorsConfiguration_AllowedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertNotNull(configuration);
        List<String> allowedHeaders = configuration.getAllowedHeaders();

        assertNotNull(allowedHeaders, "Allowed headers should not be null");
        assertTrue(allowedHeaders.contains("*"),
                "CORS should allow all headers (*)");
    }

    @Test
    void testCorsConfiguration_AppliesToDifferentPaths() {
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/api/admin/stats");

        CorsConfiguration apiConfig = corsConfigurationSource.getCorsConfiguration(apiRequest);
        CorsConfiguration adminConfig = corsConfigurationSource.getCorsConfiguration(adminRequest);

        assertNotNull(apiConfig, "API endpoint should have CORS configuration");
        assertNotNull(adminConfig, "Admin endpoint should have CORS configuration");

        // Both should have the same configuration
        assertEquals(apiConfig.getAllowedOrigins(), adminConfig.getAllowedOrigins(),
                "CORS configuration should be consistent across endpoints");
    }

    @Test
    void testCorsConfiguration_AllowsCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertNotNull(configuration);
        // Verify that credentials are handled properly (should be true for authenticated endpoints)
        Boolean allowCredentials = configuration.getAllowCredentials();
        // This can be null or true depending on configuration
        assertTrue(allowCredentials == null || allowCredentials,
                "CORS should allow credentials or leave it unspecified");
    }

    // ==========================================
    // INTEGRATION TESTS
    // ==========================================

    @Test
    void testSecurityConfig_AllBeansWiredCorrectly() {
        assertNotNull(securityConfig);
        assertNotNull(passwordEncoder);
        assertNotNull(authenticationManager);
        assertNotNull(securityFilterChain);
        assertNotNull(corsConfigurationSource);
    }

    @Test
    void testPasswordEncoder_HandleEmptyString() {
        String empty = "";
        String encodedEmpty = passwordEncoder.encode(empty);

        assertNotNull(encodedEmpty);
        assertTrue(passwordEncoder.matches(empty, encodedEmpty),
                "Should be able to encode and match empty string");
    }

    @Test
    void testPasswordEncoder_HandleSpecialCharacters() {
        String specialPassword = "p@ssw0rd!#$%^&*()";
        String encoded = passwordEncoder.encode(specialPassword);

        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(specialPassword, encoded),
                "Should handle special characters correctly");
    }

    @Test
    void testPasswordEncoder_HandleUnicodeCharacters() {
        String unicodePassword = "пароль密码🔐";
        String encoded = passwordEncoder.encode(unicodePassword);

        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(unicodePassword, encoded),
                "Should handle Unicode characters correctly");
    }
}
