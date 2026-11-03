package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testSearchUsers_Success() throws Exception {
        // Arrange
        List<UserService.UserSearchResult> results = Arrays.asList(
                new UserService.UserSearchResult(1L, "John Doe", "john@example.com", "1234567890"),
                new UserService.UserSearchResult(2L, "Jane Doe", "jane@example.com", "0987654321")
        );
        when(userService.searchUsers("john")).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"));

        verify(userService).searchUsers("john");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testSearchUsers_NoResults() throws Exception {
        // Arrange
        when(userService.searchUsers("xyz")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userService).searchUsers("xyz");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testSearchUsers_ByEmail() throws Exception {
        // Arrange
        List<UserService.UserSearchResult> results = Arrays.asList(
                new UserService.UserSearchResult(1L, "John Doe", "john@example.com", "1234567890")
        );
        when(userService.searchUsers("john@example.com")).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(userService).searchUsers("john@example.com");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testSearchUsers_ByPhone() throws Exception {
        // Arrange
        List<UserService.UserSearchResult> results = Arrays.asList(
                new UserService.UserSearchResult(1L, "John Doe", "john@example.com", "1234567890")
        );
        when(userService.searchUsers("1234567890")).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].phone").value("1234567890"));

        verify(userService).searchUsers("1234567890");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testSearchUsers_EmptyQuery() throws Exception {
        // Arrange
        when(userService.searchUsers("")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userService).searchUsers("");
    }
}
