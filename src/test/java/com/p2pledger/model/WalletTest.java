
package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {

    private WalletStatus getWalletStatus() {
        assertTrue(WalletStatus.values().length > 0,
                "WalletStatus must contain at least one value");
        return WalletStatus.values()[0];
    }

    private Wallet createWallet() {
        return Wallet.builder()
                .id(1L)
                .userId(10L)
                .balance(new BigDecimal("5000.00"))
                .todaySpending(new BigDecimal("500.00"))
                .dailyLimitResetDate(LocalDate.of(2026, 11, 3))
                .status(getWalletStatus())
                .version(1L)
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .build();
    }

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    @Test
    void testNoArgsConstructor() {
        Wallet wallet = new Wallet();

        assertNotNull(wallet);
        assertNull(wallet.getId());
        assertNull(wallet.getUserId());
        assertNull(wallet.getBalance());
        assertNull(wallet.getTodaySpending());
        assertNull(wallet.getDailyLimitResetDate());
        assertNull(wallet.getStatus());
        assertNull(wallet.getVersion());
        assertNull(wallet.getCreatedAt());
        assertNull(wallet.getUpdatedAt());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate resetDate = LocalDate.of(2026, 11, 3);
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 11, 3, 10, 0);

        Wallet wallet = new Wallet(
                1L,
                10L,
                new BigDecimal("5000.00"),
                new BigDecimal("500.00"),
                resetDate,
                getWalletStatus(),
                1L,
                createdAt,
                updatedAt
        );

        assertEquals(1L, wallet.getId());
        assertEquals(10L, wallet.getUserId());
        assertEquals(new BigDecimal("5000.00"), wallet.getBalance());
        assertEquals(new BigDecimal("500.00"), wallet.getTodaySpending());
        assertEquals(resetDate, wallet.getDailyLimitResetDate());
        assertEquals(getWalletStatus(), wallet.getStatus());
        assertEquals(1L, wallet.getVersion());
        assertEquals(createdAt, wallet.getCreatedAt());
        assertEquals(updatedAt, wallet.getUpdatedAt());
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    @Test
    void testGettersAndSetters() {
        Wallet wallet = new Wallet();

        LocalDate resetDate = LocalDate.of(2026, 11, 4);
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 2, 10, 0);
        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        wallet.setId(2L);
        wallet.setUserId(20L);
        wallet.setBalance(new BigDecimal("7500.00"));
        wallet.setTodaySpending(new BigDecimal("1000.00"));
        wallet.setDailyLimitResetDate(resetDate);
        wallet.setStatus(getWalletStatus());
        wallet.setVersion(2L);
        wallet.setCreatedAt(createdAt);
        wallet.setUpdatedAt(updatedAt);

        assertEquals(2L, wallet.getId());
        assertEquals(20L, wallet.getUserId());
        assertEquals(new BigDecimal("7500.00"), wallet.getBalance());
        assertEquals(new BigDecimal("1000.00"), wallet.getTodaySpending());
        assertEquals(resetDate, wallet.getDailyLimitResetDate());
        assertEquals(getWalletStatus(), wallet.getStatus());
        assertEquals(2L, wallet.getVersion());
        assertEquals(createdAt, wallet.getCreatedAt());
        assertEquals(updatedAt, wallet.getUpdatedAt());
    }

    // ==========================================
    // BUILDER
    // ==========================================

    @Test
    void testBuilder_AllFields() {
        Wallet wallet = createWallet();

        assertNotNull(wallet);
        assertEquals(1L, wallet.getId());
        assertEquals(10L, wallet.getUserId());
        assertEquals(new BigDecimal("5000.00"), wallet.getBalance());
        assertEquals(new BigDecimal("500.00"), wallet.getTodaySpending());
        assertEquals(LocalDate.of(2026, 11, 3),
                wallet.getDailyLimitResetDate());
        assertEquals(getWalletStatus(), wallet.getStatus());
        assertEquals(1L, wallet.getVersion());
        assertEquals(LocalDateTime.of(2026, 11, 1, 10, 0),
                wallet.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 11, 3, 10, 0),
                wallet.getUpdatedAt());
    }

    @Test
    void testBuilder_NullFields() {
        Wallet wallet = Wallet.builder()
                .id(null)
                .userId(null)
                .balance(null)
                .todaySpending(null)
                .dailyLimitResetDate(null)
                .status(null)
                .version(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        assertNull(wallet.getId());
        assertNull(wallet.getUserId());
        assertNull(wallet.getBalance());
        assertNull(wallet.getTodaySpending());
        assertNull(wallet.getDailyLimitResetDate());
        assertNull(wallet.getStatus());
        assertNull(wallet.getVersion());
        assertNull(wallet.getCreatedAt());
        assertNull(wallet.getUpdatedAt());
    }

    // ==========================================
    // EQUALS
    // ==========================================

    @Test
    void testEquals_SameObject() {
        Wallet wallet = createWallet();
        assertEquals(wallet, wallet);
    }

    @Test
    void testEquals_NullObject() {
        Wallet wallet = createWallet();
        assertNotEquals(null, wallet);
    }

    @Test
    void testEquals_DifferentClass() {
        Wallet wallet = createWallet();
        assertNotEquals(wallet, "Wallet");
    }

    @Test
    void testEquals_EqualObjects() {
        Wallet first = createWallet();
        Wallet second = createWallet();

        assertEquals(first, second);
    }

    @Test
    void testEquals_BothObjectsWithNullFields() {
        Wallet first = new Wallet();
        Wallet second = new Wallet();

        assertEquals(first, second);
    }

    @Test
    void testEquals_DifferentValuesForEveryField() {
        Wallet original = createWallet();

        assertNotEqual(original, w -> w.setId(2L));
        assertNotEqual(original, w -> w.setUserId(20L));
        assertNotEqual(original, w ->
                w.setBalance(new BigDecimal("6000.00")));
        assertNotEqual(original, w ->
                w.setTodaySpending(new BigDecimal("600.00")));
        assertNotEqual(original, w ->
                w.setDailyLimitResetDate(LocalDate.of(2026, 11, 4)));

        if (WalletStatus.values().length > 1) {
            assertNotEqual(original, w ->
                    w.setStatus(WalletStatus.values()[1]));
        }

        assertNotEqual(original, w -> w.setVersion(2L));
        assertNotEqual(original, w ->
                w.setCreatedAt(LocalDateTime.of(2026, 11, 2, 10, 0)));
        assertNotEqual(original, w ->
                w.setUpdatedAt(LocalDateTime.of(2026, 11, 4, 10, 0)));
    }

    private void assertNotEqual(
            Wallet original,
            Consumer<Wallet> change) {

        Wallet modified = createWallet();
        change.accept(modified);

        assertNotEquals(original, modified);
    }

    @Test
    void testEquals_NullFieldVersusNonNullField() {
        Wallet first = new Wallet();
        Wallet second = new Wallet();

        second.setBalance(new BigDecimal("100.00"));
        assertNotEquals(first, second);

        first.setBalance(new BigDecimal("100.00"));
        assertEquals(first, second);
    }

    @Test
    void testCanEqual_WithWallet() {
        Wallet wallet = createWallet();
        assertTrue(wallet.canEqual(new Wallet()));
    }

    @Test
    void testCanEqual_WithDifferentClass() {
        Wallet wallet = createWallet();
        assertFalse(wallet.canEqual("Not a Wallet"));
    }

    @Test
    void testEquals_WhenOtherObjectRejectsCanEqual() {
        Wallet wallet = createWallet();

        Wallet other = new Wallet() {
            @Override
            public boolean canEqual(Object object) {
                return false;
            }
        };

        assertNotEquals(wallet, other);
    }

    // ==========================================
    // HASH CODE
    // ==========================================

    @Test
    void testHashCode_EqualObjects() {
        Wallet first = createWallet();
        Wallet second = createWallet();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHashCode_NullFields() {
        Wallet wallet = new Wallet();
        assertDoesNotThrow(wallet::hashCode);
    }

    @Test
    void testHashCode_NonNullFields() {
        Wallet wallet = createWallet();
        assertDoesNotThrow(wallet::hashCode);
    }

    // ==========================================
    // TO STRING
    // ==========================================

    @Test
    void testToString_AllFields() {
        Wallet wallet = createWallet();

        String result = wallet.toString();

        assertNotNull(result);
        assertTrue(result.contains("Wallet"));
        assertTrue(result.contains("id="));
        assertTrue(result.contains("userId="));
        assertTrue(result.contains("balance="));
        assertTrue(result.contains("todaySpending="));
        assertTrue(result.contains("dailyLimitResetDate="));
        assertTrue(result.contains("status="));
        assertTrue(result.contains("version="));
        assertTrue(result.contains("createdAt="));
        assertTrue(result.contains("updatedAt="));
    }

    @Test
    void testToString_NullFields() {
        Wallet wallet = new Wallet();

        String result = wallet.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=null"));
        assertTrue(result.contains("userId=null"));
        assertTrue(result.contains("balance=null"));
        assertTrue(result.contains("todaySpending=null"));
        assertTrue(result.contains("dailyLimitResetDate=null"));
        assertTrue(result.contains("status=null"));
        assertTrue(result.contains("version=null"));
        assertTrue(result.contains("createdAt=null"));
        assertTrue(result.contains("updatedAt=null"));
    }
}
