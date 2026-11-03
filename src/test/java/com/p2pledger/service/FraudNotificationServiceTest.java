package com.p2pledger.service;

import com.p2pledger.model.Notification;
import com.p2pledger.model.NotificationType;
import com.p2pledger.model.Role;
import com.p2pledger.model.User;
import com.p2pledger.repository.NotificationRepository;
import com.p2pledger.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FraudNotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private FraudNotificationService fraudNotificationService;

    private User admin1;
    private User admin2;
    private User normalUser;

    @BeforeEach
    void setUp() {
        admin1 = new User();
        admin1.setId(1L);
        admin1.setRole(Role.ADMIN);

        admin2 = new User();
        admin2.setId(2L);
        admin2.setRole(Role.ADMIN);

        normalUser = new User();
        normalUser.setId(3L);
        normalUser.setRole(Role.USER);
    }

    // -------------------------------------------------------
    // DAILY LIMIT VIOLATION
    // -------------------------------------------------------

    @Test
    void testNotifyAdminsAboutDailyLimitViolation_Success() {

        when(userRepository.findAll())
                .thenReturn(List.of(admin1, normalUser, admin2));

        BigDecimal attemptedAmount = new BigDecimal("500");
        BigDecimal todaySpending = new BigDecimal("900");
        BigDecimal dailyLimit = new BigDecimal("1000");

        fraudNotificationService.notifyAdminsAboutDailyLimitViolation(
                10L,
                "Test Sender",
                attemptedAmount,
                todaySpending,
                dailyLimit
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository, times(2)).save(captor.capture());

        List<Notification> savedNotifications = captor.getAllValues();

        assertEquals(2, savedNotifications.size());

        Notification first = savedNotifications.get(0);
        Notification second = savedNotifications.get(1);

        assertEquals(1L, first.getUserId());
        assertEquals(2L, second.getUserId());

        for (Notification notification : savedNotifications) {
            assertEquals(NotificationType.FRAUD_ALERT, notification.getType());
            assertFalse(notification.isRead());
            assertNotNull(notification.getCreatedAt());

            assertTrue(notification.getMessage().contains("Daily limit violation"));
            assertTrue(notification.getMessage().contains("User #10"));
            assertTrue(notification.getMessage().contains("Test Sender"));
            assertTrue(notification.getMessage().contains("₹500"));
            assertTrue(notification.getMessage().contains("₹900"));
            assertTrue(notification.getMessage().contains("₹1000"));
            assertTrue(notification.getMessage().contains("₹400"));
        }

        verify(userRepository).findAll();
    }

    @Test
    void testNotifyAdminsAboutDailyLimitViolation_NoAdmins() {

        when(userRepository.findAll())
                .thenReturn(List.of(normalUser));

        fraudNotificationService.notifyAdminsAboutDailyLimitViolation(
                10L,
                "Test Sender",
                new BigDecimal("500"),
                new BigDecimal("900"),
                new BigDecimal("1000")
        );

        verify(userRepository).findAll();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testNotifyAdminsAboutDailyLimitViolation_EmptyUserList() {

        when(userRepository.findAll())
                .thenReturn(List.of());

        fraudNotificationService.notifyAdminsAboutDailyLimitViolation(
                10L,
                "Test Sender",
                new BigDecimal("500"),
                new BigDecimal("900"),
                new BigDecimal("1000")
        );

        verify(userRepository).findAll();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testNotifyAdminsAboutDailyLimitViolation_OnlyAdmins() {

        when(userRepository.findAll())
                .thenReturn(List.of(admin1, admin2));

        fraudNotificationService.notifyAdminsAboutDailyLimitViolation(
                10L,
                "Test Sender",
                new BigDecimal("200"),
                new BigDecimal("300"),
                new BigDecimal("1000")
        );

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    // -------------------------------------------------------
    // DUPLICATE TRANSFER
    // -------------------------------------------------------

    @Test
    void testNotifyAdminsAboutDuplicateTransfer_Success() {

        when(userRepository.findAll())
                .thenReturn(List.of(admin1, normalUser, admin2));

        BigDecimal amount = new BigDecimal("750");

        fraudNotificationService.notifyAdminsAboutDuplicateTransfer(
                10L,
                "Sender",
                20L,
                "Receiver",
                amount
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository, times(2)).save(captor.capture());

        List<Notification> savedNotifications = captor.getAllValues();

        assertEquals(2, savedNotifications.size());

        assertEquals(1L, savedNotifications.get(0).getUserId());
        assertEquals(2L, savedNotifications.get(1).getUserId());

        for (Notification notification : savedNotifications) {
            assertEquals(NotificationType.FRAUD_ALERT, notification.getType());
            assertFalse(notification.isRead());
            assertNotNull(notification.getCreatedAt());

            assertTrue(notification.getMessage().contains("Duplicate transaction detected"));
            assertTrue(notification.getMessage().contains("User #10"));
            assertTrue(notification.getMessage().contains("Sender"));
            assertTrue(notification.getMessage().contains("₹750"));
            assertTrue(notification.getMessage().contains("User #20"));
            assertTrue(notification.getMessage().contains("Receiver"));
            assertTrue(notification.getMessage().contains("within 2 minutes"));
        }

        verify(userRepository).findAll();
    }

    @Test
    void testNotifyAdminsAboutDuplicateTransfer_NoAdmins() {

        when(userRepository.findAll())
                .thenReturn(List.of(normalUser));

        fraudNotificationService.notifyAdminsAboutDuplicateTransfer(
                10L,
                "Sender",
                20L,
                "Receiver",
                new BigDecimal("750")
        );

        verify(userRepository).findAll();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testNotifyAdminsAboutDuplicateTransfer_EmptyUserList() {

        when(userRepository.findAll())
                .thenReturn(List.of());

        fraudNotificationService.notifyAdminsAboutDuplicateTransfer(
                10L,
                "Sender",
                20L,
                "Receiver",
                new BigDecimal("750")
        );

        verify(userRepository).findAll();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testNotifyAdminsAboutDuplicateTransfer_OnlyAdmins() {

        when(userRepository.findAll())
                .thenReturn(List.of(admin1, admin2));

        fraudNotificationService.notifyAdminsAboutDuplicateTransfer(
                10L,
                "Sender",
                20L,
                "Receiver",
                new BigDecimal("750")
        );

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}