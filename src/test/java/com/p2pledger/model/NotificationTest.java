package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationTest {

    private NotificationType getType() {
        return NotificationType.values()[0];
    }

    private Notification createNotification() {
        return Notification.builder()
                .id(1L)
                .userId(10L)
                .type(getType())
                .message("Your transfer was successful")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        Notification notification = new Notification();

        assertNull(notification.getId());
        assertNull(notification.getUserId());
        assertNull(notification.getType());
        assertNull(notification.getMessage());
        assertFalse(notification.isRead());
        assertNull(notification.getCreatedAt());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        Notification notification = new Notification(
                1L,
                10L,
                getType(),
                "Your transfer was successful",
                true,
                createdAt
        );

        assertEquals(1L, notification.getId());
        assertEquals(10L, notification.getUserId());
        assertEquals(getType(), notification.getType());
        assertEquals("Your transfer was successful", notification.getMessage());
        assertTrue(notification.isRead());
        assertEquals(createdAt, notification.getCreatedAt());
    }

    @Test
    void gettersAndSettersTest() {
        Notification notification = new Notification();
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        notification.setId(1L);
        notification.setUserId(10L);
        notification.setType(getType());
        notification.setMessage("Your transfer was successful");
        notification.setRead(true);
        notification.setCreatedAt(createdAt);

        assertEquals(1L, notification.getId());
        assertEquals(10L, notification.getUserId());
        assertEquals(getType(), notification.getType());
        assertEquals("Your transfer was successful", notification.getMessage());
        assertTrue(notification.isRead());
        assertEquals(createdAt, notification.getCreatedAt());
    }

    @Test
    void customSetReadTest() {
        Notification notification = createNotification();

        notification.setRead(true);
        assertTrue(notification.isRead());

        notification.setRead(false);
        assertFalse(notification.isRead());
    }

    @Test
    void builderTest() {
        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(getType())
                .message("Your transfer was successful")
                .isRead(true)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();

        assertEquals(1L, notification.getId());
        assertEquals(10L, notification.getUserId());
        assertEquals(getType(), notification.getType());
        assertEquals("Your transfer was successful", notification.getMessage());
        assertTrue(notification.isRead());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 10, 0),
                notification.getCreatedAt()
        );
    }

    @Test
    void builderWithReadFalseTest() {
        Notification notification = Notification.builder()
                .id(2L)
                .userId(20L)
                .type(getType())
                .message("New notification")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        assertFalse(notification.isRead());
    }

    @Test
    void equalsSameObjectTest() {
        Notification notification = createNotification();

        assertEquals(notification, notification);
    }

    @Test
    void equalsEqualObjectsTest() {
        Notification notification1 = createNotification();
        Notification notification2 = createNotification();

        assertEquals(notification1, notification2);
    }

    @Test
    void equalsDifferentObjectsTest() {
        Notification notification1 = createNotification();

        Notification notification2 = Notification.builder()
                .id(2L)
                .userId(20L)
                .type(getType())
                .message("Different notification")
                .isRead(true)
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        assertNotEquals(notification1, notification2);
    }

    @Test
    void equalsNullTest() {
        Notification notification = createNotification();

        assertNotEquals(null, notification);
    }

    @Test
    void canEqualTest() {
        Notification notification = createNotification();

        assertTrue(notification.canEqual(createNotification()));
        assertFalse(notification.canEqual("not a notification"));
        assertFalse(notification.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        Notification notification = createNotification();

        Notification other = new Notification() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(notification, other);
    }

    @Test
    void hashCodeTest() {
        Notification notification1 = createNotification();
        Notification notification2 = createNotification();

        assertEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    void toStringTest() {
        Notification notification = createNotification();

        String result = notification.toString();

        assertTrue(result.contains("Your transfer was successful"));
        assertTrue(result.contains("userId=10"));
        assertTrue(result.contains("isRead=false"));
    }

    @Test
    void equalsDifferentReadValueTest() {
        Notification notification1 = createNotification();
        Notification notification2 = createNotification();

        notification2.setRead(true);

        assertNotEquals(notification1, notification2);
    }

    @Test
    void equalsDifferentTypeTest() {
        NotificationType[] types = NotificationType.values();

        if (types.length > 1) {
            Notification notification1 = createNotification();
            Notification notification2 = createNotification();

            notification2.setType(types[1]);

            assertNotEquals(notification1, notification2);
        }
    }
}