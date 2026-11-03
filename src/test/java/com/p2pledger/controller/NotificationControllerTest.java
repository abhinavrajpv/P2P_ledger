package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.model.Notification;
import com.p2pledger.model.NotificationType;
import com.p2pledger.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.TRANSFER_INITIATED)
                .message("Test notification")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetUnreadCount_Success() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(1L)).thenReturn(5);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(notificationService).getUnreadCount(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetUnreadCount_Zero() throws Exception {
        // Arrange
        when(notificationService.getUnreadCount(1L)).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(notificationService).getUnreadCount(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetNotifications_Success() throws Exception {
        // Arrange
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification), 
                PageRequest.of(0, 10), 1);
        when(notificationService.getNotifications(eq(1L), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Test notification"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetNotifications_WithPagination() throws Exception {
        // Arrange
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification), 
                PageRequest.of(2, 5), 15);
        when(notificationService.getNotifications(eq(1L), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .param("page", "2")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetNotifications_EmptyList() throws Exception {
        // Arrange
        Page<Notification> page = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(notificationService.getNotifications(eq(1L), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(notificationService).getNotifications(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testMarkAsRead_Success() throws Exception {
        // Arrange
        doNothing().when(notificationService).markAsRead(1L, 1L);

        // Act & Assert
        mockMvc.perform(patch("/api/notifications/1/read")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testMarkAsRead_NotificationNotFound() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Notification not found"))
                .when(notificationService).markAsRead(999L, 1L);

        // Act & Assert
        mockMvc.perform(patch("/api/notifications/999/read")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(notificationService).markAsRead(999L, 1L);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void testMarkAsRead_WrongUser() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Access denied"))
                .when(notificationService).markAsRead(1L, 2L);

        // Act & Assert
        mockMvc.perform(patch("/api/notifications/1/read")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(notificationService).markAsRead(1L, 2L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetNotifications_DefaultPagination() throws Exception {
        // Arrange
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification), 
                PageRequest.of(0, 10), 1);
        when(notificationService.getNotifications(eq(1L), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(notificationService).getNotifications(eq(1L), any());
    }
}
