
package com.p2pledger.config;

import com.p2pledger.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // NO AUTHORIZATION HEADER
    // ==========================================

    @Test
    void testDoFilter_NoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // AUTHORIZATION HEADER WITHOUT BEARER
    // ==========================================

    @Test
    void testDoFilter_NonBearerAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Basic someCredentials");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // EMPTY BEARER TOKEN
    // ==========================================

    @Test
    void testDoFilter_EmptyBearerToken() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer ");

        when(jwtService.validateToken(""))
                .thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService).validateToken("");
        verify(jwtService, never()).extractUserId(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // INVALID TOKEN
    // ==========================================

    @Test
    void testDoFilter_InvalidToken() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer invalidToken");

        when(jwtService.validateToken("invalidToken"))
                .thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService).validateToken("invalidToken");
        verify(jwtService, never()).extractUserId(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // VALID USER TOKEN
    // ==========================================

    @Test
    void testDoFilter_ValidUserToken() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer validToken");

        when(jwtService.validateToken("validToken"))
                .thenReturn(true);

        when(jwtService.extractUserId("validToken"))
                .thenReturn(1L);

        when(jwtService.extractRole("validToken"))
                .thenReturn("USER");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(1L, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertTrue(authentication.isAuthenticated());

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_USER"))
        );

        verify(jwtService).validateToken("validToken");
        verify(jwtService).extractUserId("validToken");
        verify(jwtService).extractRole("validToken");
        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // VALID ADMIN TOKEN
    // ==========================================

    @Test
    void testDoFilter_ValidAdminToken() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer adminToken");

        when(jwtService.validateToken("adminToken"))
                .thenReturn(true);

        when(jwtService.extractUserId("adminToken"))
                .thenReturn(2L);

        when(jwtService.extractRole("adminToken"))
                .thenReturn("ADMIN");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(2L, authentication.getPrincipal());

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_ADMIN"))
        );

        verify(filterChain).doFilter(request, response);
    }

    // ==========================================
    // FILTER CHAIN EXCEPTION PROPAGATION
    // ==========================================

    @Test
    void testDoFilter_FilterChainThrowsServletException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        doThrow(new ServletException("Filter chain error"))
                .when(filterChain)
                .doFilter(request, response);

        ServletException exception = assertThrows(
                ServletException.class,
                () -> jwtAuthenticationFilter.doFilter(
                        request, response, filterChain)
        );

        assertEquals("Filter chain error", exception.getMessage());
    }

    @Test
    void testDoFilter_FilterChainThrowsIOException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        doThrow(new IOException("IO error"))
                .when(filterChain)
                .doFilter(request, response);

        IOException exception = assertThrows(
                IOException.class,
                () -> jwtAuthenticationFilter.doFilter(
                        request, response, filterChain)
        );

        assertEquals("IO error", exception.getMessage());
    }
}
