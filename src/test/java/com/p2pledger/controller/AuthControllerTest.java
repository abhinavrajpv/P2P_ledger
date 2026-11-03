
package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.model.Role;
import com.p2pledger.model.User;
import com.p2pledger.service.AuthService;
import com.p2pledger.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
    }

    @Test
    void testRegister_Success() throws Exception {

        AuthController.RegisterRequest request =
                new AuthController.RegisterRequest();

        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPhone("1234567890");
        request.setPassword("password123");
        request.setTransactionPin("1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("User registered successfully"));

        verify(authService).register(
                "Test User",
                "test@example.com",
                "1234567890",
                "password123",
                "1234"
        );
    }

    @Test
    void testRegister_ValidationFailure() throws Exception {

        AuthController.RegisterRequest request =
                new AuthController.RegisterRequest();

        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPhone("1234567890");
        request.setPassword("password123");
        request.setTransactionPin("1234");

        when(authService.register(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new IllegalArgumentException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService).register(
                "Test User",
                "test@example.com",
                "1234567890",
                "password123",
                "1234"
        );
    }

    @Test
    void testLogin_Success() throws Exception {

        AuthController.LoginRequest request =
                new AuthController.LoginRequest();

        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        when(authService.login(
                "test@example.com",
                "password123"
        )).thenReturn(testUser);

        when(jwtService.generateToken(1L, "USER"))
                .thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        verify(authService).login(
                "test@example.com",
                "password123"
        );

        verify(jwtService).generateToken(1L, "USER");
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {

        AuthController.LoginRequest request =
                new AuthController.LoginRequest();

        request.setIdentifier("test@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(
                "test@example.com",
                "wrongpassword"
        )).thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService).login(
                "test@example.com",
                "wrongpassword"
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void testLogin_AdminRole() throws Exception {

        testUser.setRole(Role.ADMIN);

        AuthController.LoginRequest request =
                new AuthController.LoginRequest();

        request.setIdentifier("admin@example.com");
        request.setPassword("adminpass");

        when(authService.login(
                "admin@example.com",
                "adminpass"
        )).thenReturn(testUser);

        when(jwtService.generateToken(1L, "ADMIN"))
                .thenReturn("admin.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        verify(authService).login(
                "admin@example.com",
                "adminpass"
        );

        verify(jwtService).generateToken(1L, "ADMIN");
    }
}
