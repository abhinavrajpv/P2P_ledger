
package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User createUser() {
        return User.builder()
                .id(1L)
                .name("John")
                .email("john@example.com")
                .phone("9876543210")
                .password("hashedPassword")
                .transactionPin("hashedPin")
                .role(Role.USER)
                .maskingEnabled(true)
                .failedLoginCount(2)
                .lockedUntil(LocalDateTime.of(2026, 11, 3, 10, 0))
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .build();
    }

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    @Test
    void testNoArgsConstructor() {
        User user = new User();

        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        assertNull(user.getPassword());
        assertNull(user.getTransactionPin());
        assertNull(user.getRole());
        assertFalse(user.isMaskingEnabled());
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime lockedUntil =
                LocalDateTime.of(2026, 11, 3, 10, 0);
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 11, 3, 10, 0);

        User user = new User(
                1L,
                "John",
                "john@example.com",
                "9876543210",
                "hashedPassword",
                "hashedPin",
                Role.USER,
                true,
                2,
                lockedUntil,
                createdAt,
                updatedAt
        );

        assertEquals(1L, user.getId());
        assertEquals("John", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("9876543210", user.getPhone());
        assertEquals("hashedPassword", user.getPassword());
        assertEquals("hashedPin", user.getTransactionPin());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isMaskingEnabled());
        assertEquals(2, user.getFailedLoginCount());
        assertEquals(lockedUntil, user.getLockedUntil());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    @Test
    void testGettersAndSetters() {
        User user = new User();

        LocalDateTime lockedUntil =
                LocalDateTime.of(2026, 11, 4, 10, 0);
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        user.setId(2L);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPhone("9876543211");
        user.setPassword("password");
        user.setTransactionPin("123456");
        user.setRole(Role.ADMIN);
        user.setMaskingEnabled(false);
        user.setFailedLoginCount(5);
        user.setLockedUntil(lockedUntil);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertEquals(2L, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("9876543211", user.getPhone());
        assertEquals("password", user.getPassword());
        assertEquals("123456", user.getTransactionPin());
        assertEquals(Role.ADMIN, user.getRole());
        assertFalse(user.isMaskingEnabled());
        assertEquals(5, user.getFailedLoginCount());
        assertEquals(lockedUntil, user.getLockedUntil());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    void testSetMaskingEnabledTrueAndFalse() {
        User user = new User();

        user.setMaskingEnabled(true);
        assertTrue(user.isMaskingEnabled());

        user.setMaskingEnabled(false);
        assertFalse(user.isMaskingEnabled());
    }

    @Test
    void testSetFailedLoginCount() {
        User user = new User();

        user.setFailedLoginCount(1);
        assertEquals(1, user.getFailedLoginCount());

        user.setFailedLoginCount(0);
        assertEquals(0, user.getFailedLoginCount());
    }

    // ==========================================
    // BUILDER
    // ==========================================

    @Test
    void testBuilder_AllFields() {
        User user = createUser();

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("John", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("9876543210", user.getPhone());
        assertEquals("hashedPassword", user.getPassword());
        assertEquals("hashedPin", user.getTransactionPin());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isMaskingEnabled());
        assertEquals(2, user.getFailedLoginCount());
        assertNotNull(user.getLockedUntil());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void testBuilder_NullAndDefaultFields() {
        User user = User.builder()
                .id(null)
                .name(null)
                .email(null)
                .phone(null)
                .password(null)
                .transactionPin(null)
                .role(null)
                .lockedUntil(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        assertNull(user.getPassword());
        assertNull(user.getTransactionPin());
        assertNull(user.getRole());
        assertFalse(user.isMaskingEnabled());
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void testBuilder_PrimitiveFields() {
        User user = User.builder()
                .maskingEnabled(true)
                .failedLoginCount(10)
                .build();

        assertTrue(user.isMaskingEnabled());
        assertEquals(10, user.getFailedLoginCount());
    }

    // ==========================================
    // EQUALS
    // ==========================================

    @Test
    void testEquals_SameObject() {
        User user = createUser();

        assertEquals(user, user);
    }

    @Test
    void testEquals_NullObject() {
        User user = createUser();

        assertNotEquals(null, user);
    }

    @Test
    void testEquals_DifferentClass() {
        User user = createUser();

        assertNotEquals(user, "User");
    }

    @Test
    void testEquals_EqualObjects() {
        User first = createUser();
        User second = createUser();

        assertEquals(first, second);
    }

    @Test
    void testEquals_BothObjectsWithNullFields() {
        User first = new User();
        User second = new User();

        assertEquals(first, second);
    }

    @Test
    void testEquals_DifferentValuesForEveryField() {
        User original = createUser();

        assertNotEqual(original, u -> u.setId(2L));
        assertNotEqual(original, u -> u.setName("Alice"));
        assertNotEqual(original, u -> u.setEmail("alice@example.com"));
        assertNotEqual(original, u -> u.setPhone("9876543211"));
        assertNotEqual(original, u -> u.setPassword("differentPassword"));
        assertNotEqual(original, u -> u.setTransactionPin("654321"));
        assertNotEqual(original, u -> u.setRole(Role.ADMIN));
        assertNotEqual(original, u -> u.setMaskingEnabled(false));
        assertNotEqual(original, u -> u.setFailedLoginCount(3));
        assertNotEqual(original, u -> u.setLockedUntil(
                LocalDateTime.of(2026, 11, 4, 10, 0)));
        assertNotEqual(original, u -> u.setCreatedAt(
                LocalDateTime.of(2026, 11, 2, 10, 0)));
        assertNotEqual(original, u -> u.setUpdatedAt(
                LocalDateTime.of(2026, 11, 4, 10, 0)));
    }

    private void assertNotEqual(
            User original,
            Consumer<User> change) {

        User modified = createUser();
        change.accept(modified);

        assertNotEquals(original, modified);
    }

    @Test
    void testEquals_NullFieldVersusNonNullField() {
        User first = new User();
        User second = new User();

        second.setName("John");
        assertNotEquals(first, second);

        first.setName("John");
        assertEquals(first, second);
    }

    @Test
    void testCanEqual_WithUser() {
        User user = createUser();

        assertTrue(user.canEqual(new User()));
    }

    @Test
    void testCanEqual_WithDifferentClass() {
        User user = createUser();

        assertFalse(user.canEqual("Not a User"));
    }

    @Test
    void testEquals_WhenOtherObjectRejectsCanEqual() {
        User user = createUser();

        User other = new User() {
            @Override
            public boolean canEqual(Object object) {
                return false;
            }
        };

        assertNotEquals(user, other);
    }

    // ==========================================
    // HASH CODE
    // ==========================================

    @Test
    void testHashCode_EqualObjects() {
        User first = createUser();
        User second = createUser();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHashCode_NullFields() {
        User user = new User();

        assertDoesNotThrow(user::hashCode);
    }

    @Test
    void testHashCode_NonNullFields() {
        User user = createUser();

        assertDoesNotThrow(user::hashCode);
    }

    @Test
    void testHashCode_DifferentObjects() {
        User first = createUser();

        User second = createUser();
        second.setId(2L);

        assertNotEquals(first.hashCode(), second.hashCode());
    }

    // ==========================================
    // TO STRING
    // ==========================================

    @Test
    void testToString_AllFields() {
        User user = createUser();

        String result = user.toString();

        assertNotNull(result);
        assertTrue(result.contains("User"));
        assertTrue(result.contains("id="));
        assertTrue(result.contains("name="));
        assertTrue(result.contains("email="));
        assertTrue(result.contains("phone="));
        assertTrue(result.contains("password="));
        assertTrue(result.contains("transactionPin="));
        assertTrue(result.contains("role="));
        assertTrue(result.contains("maskingEnabled="));
        assertTrue(result.contains("failedLoginCount="));
        assertTrue(result.contains("lockedUntil="));
        assertTrue(result.contains("createdAt="));
        assertTrue(result.contains("updatedAt="));
    }

    @Test
    void testToString_NullFields() {
        User user = new User();

        String result = user.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=null"));
        assertTrue(result.contains("name=null"));
        assertTrue(result.contains("email=null"));
        assertTrue(result.contains("role=null"));
        assertTrue(result.contains("maskingEnabled=false"));
        assertTrue(result.contains("failedLoginCount=0"));
    }
}
