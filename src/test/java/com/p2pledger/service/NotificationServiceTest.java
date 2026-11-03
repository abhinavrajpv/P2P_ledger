package com.p2pledger.service;

import com.p2pledger.model.Notification;
import com.p2pledger.model.NotificationType;
import com.p2pledger.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification1;
    private Notification notification2;
    private Notification notification3;

    private final Long userId = 1L;
    private final Long otherUserId = 2L;

    private NotificationType notificationType;

    @BeforeEach
    void setUp() {
        notificationType = NotificationType.values()[0];

        notification1 = createNotification(
                1L, userId, "First notification",
                LocalDateTime.of(2026, 10, 1, 10, 0), false
        );

        notification2 = createNotification(
                2L, userId, "Second notification",
                LocalDateTime.of(2026, 10, 2, 10, 0), false
        );

        notification3 = createNotification(
                3L, userId, "Third notification",
                LocalDateTime.of(2026, 10, 3, 10, 0), true
        );
    }

    // -------------------------------------------------------
    // CREATE NOTIFICATION
    // -------------------------------------------------------

    @Test
    void testCreateNotification_Success() {

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.createNotification(
                userId,
                notificationType,
                "Payment received"
        );

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(notificationType, result.getType());
        assertEquals("Payment received", result.getMessage());
        assertFalse(result.isRead());
        assertNotNull(result.getCreatedAt());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_SavesCorrectNotification() {

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createNotification(
                userId,
                notificationType,
                "Test message"
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals(notificationType, saved.getType());
        assertEquals("Test message", saved.getMessage());
        assertFalse(saved.isRead());
    }

    // -------------------------------------------------------
    // MARK NOTIFICATION AS READ
    // -------------------------------------------------------

    @Test
    void testMarkAsRead_Success() {

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification1));

        notificationService.markAsRead(1L, userId);

        assertTrue(notification1.isRead());

        verify(notificationRepository).findById(1L);
        verify(notificationRepository).save(notification1);
    }

    @Test
    void testMarkAsRead_NotificationNotFound() {

        when(notificationRepository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsRead(99L, userId)
        );

        assertEquals("Notification not found", exception.getMessage());

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_AccessDenied() {

        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsRead(1L, otherUserId)
        );

        assertEquals("Access denied", exception.getMessage());

        assertFalse(notification1.isRead());

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // -------------------------------------------------------
    // MARK ALL NOTIFICATIONS AS READ
    // -------------------------------------------------------

    @Test
    void testMarkAllAsRead_Success() {

        List<Notification> notifications = new ArrayList<>(
                List.of(notification1, notification2, notification3)
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        notificationService.markAllAsRead(userId);

        assertTrue(notification1.isRead());
        assertTrue(notification2.isRead());
        assertTrue(notification3.isRead());

        verify(notificationRepository)
                .findByUserIdOrderByCreatedAtDesc(userId);

        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    void testMarkAllAsRead_EmptyList() {

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>());

        notificationService.markAllAsRead(userId);

        verify(notificationRepository)
                .findByUserIdOrderByCreatedAtDesc(userId);

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void testMarkAllAsRead_AlreadyReadNotifications() {

        List<Notification> notifications = new ArrayList<>(
                List.of(notification3)
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        notificationService.markAllAsRead(userId);

        assertTrue(notification3.isRead());

        verify(notificationRepository).saveAll(notifications);
    }

    // -------------------------------------------------------
    // GET UNREAD COUNT
    // -------------------------------------------------------

    @Test
    void testGetUnreadCount_Success() {

        when(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .thenReturn(5);

        int result = notificationService.getUnreadCount(userId);

        assertEquals(5, result);

        verify(notificationRepository)
                .countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void testGetUnreadCount_Zero() {

        when(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .thenReturn(0);

        int result = notificationService.getUnreadCount(userId);

        assertEquals(0, result);

        verify(notificationRepository)
                .countByUserIdAndIsReadFalse(userId);
    }

    // -------------------------------------------------------
    // GET PAGINATED NOTIFICATIONS
    // -------------------------------------------------------

    @Test
    void testGetNotifications_Paginated_Success() {

        List<Notification> notifications = List.of(
                notification1,
                notification2,
                notification3
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        Pageable pageable = PageRequest.of(0, 2);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());

        // Most recent notification should appear first.
        assertEquals(3L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());

        verify(notificationRepository)
                .findByUserIdOrderByCreatedAtDesc(userId);
    }
    @Test
    void testGetNotifications_Paginated_SecondPage() {

        List<Notification> notifications = List.of(
                notification1,
                notification2,
                notification3
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        Pageable pageable = PageRequest.of(1, 2);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void testGetNotifications_Paginated_PageBeyondAvailableRecords() {

        List<Notification> notifications = List.of(
                notification1,
                notification2,
                notification3
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        Pageable pageable = PageRequest.of(5, 2);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertTrue(result.getContent().isEmpty());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testGetNotifications_Paginated_EmptyList() {

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>());

        Pageable pageable = PageRequest.of(0, 5);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
    }

    @Test
    void testGetNotifications_Paginated_ExactPageSize() {

        List<Notification> notifications = List.of(
                notification1,
                notification2
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        Pageable pageable = PageRequest.of(0, 2);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void testGetNotifications_Paginated_SingleNotification() {

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification1));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result =
                notificationService.getNotifications(userId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    // -------------------------------------------------------
    // GET ALL NOTIFICATIONS WITHOUT PAGINATION
    // -------------------------------------------------------

    @Test
    void testGetNotifications_WithoutPagination_Success() {

        List<Notification> notifications = List.of(
                notification1,
                notification2,
                notification3
        );

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        List<Notification> result =
                notificationService.getNotifications(userId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(notifications, result);

        verify(notificationRepository)
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void testGetNotifications_WithoutPagination_EmptyList() {

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>());

        List<Notification> result =
                notificationService.getNotifications(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository)
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    // -------------------------------------------------------
    // HELPER METHOD
    // -------------------------------------------------------

    private Notification createNotification(
            Long id,
            Long userId,
            String message,
            LocalDateTime createdAt,
            boolean isRead
    ) {

        return Notification.builder()
                .id(id)
                .userId(userId)
                .type(notificationType)
                .message(message)
                .isRead(isRead)
                .createdAt(createdAt)
                .build();
    }
}