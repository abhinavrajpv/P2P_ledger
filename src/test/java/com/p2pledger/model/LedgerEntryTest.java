
package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerEntryTest {

    private LedgerType getLedgerType() {
        assertTrue(LedgerType.values().length > 0,
                "LedgerType must contain at least one enum value");

        return LedgerType.values()[0];
    }

    private LedgerEntry createLedgerEntry() {
        return LedgerEntry.builder()
                .id(1L)
                .transferId(100L)
                .userId(10L)
                .type(getLedgerType())
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("500.00"))
                .referenceId("REF-001")
                .createdAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .build();
    }

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    @Test
    void testNoArgsConstructor() {
        LedgerEntry entry = new LedgerEntry();

        assertNotNull(entry);
        assertNull(entry.getId());
        assertNull(entry.getTransferId());
        assertNull(entry.getUserId());
        assertNull(entry.getType());
        assertNull(entry.getAmount());
        assertNull(entry.getBalanceBefore());
        assertNull(entry.getBalanceAfter());
        assertNull(entry.getReferenceId());
        assertNull(entry.getCreatedAt());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 3, 10, 0);

        LedgerEntry entry = new LedgerEntry(
                1L,
                100L,
                10L,
                getLedgerType(),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                "REF-001",
                createdAt
        );

        assertEquals(1L, entry.getId());
        assertEquals(100L, entry.getTransferId());
        assertEquals(10L, entry.getUserId());
        assertEquals(getLedgerType(), entry.getType());
        assertEquals(new BigDecimal("500.00"), entry.getAmount());
        assertEquals(new BigDecimal("1000.00"), entry.getBalanceBefore());
        assertEquals(new BigDecimal("500.00"), entry.getBalanceAfter());
        assertEquals("REF-001", entry.getReferenceId());
        assertEquals(createdAt, entry.getCreatedAt());
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    @Test
    void testGettersAndSetters() {
        LedgerEntry entry = new LedgerEntry();

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 4, 11, 0);

        entry.setId(2L);
        entry.setTransferId(200L);
        entry.setUserId(20L);
        entry.setType(getLedgerType());
        entry.setAmount(new BigDecimal("750.00"));
        entry.setBalanceBefore(new BigDecimal("2000.00"));
        entry.setBalanceAfter(new BigDecimal("1250.00"));
        entry.setReferenceId("REF-002");
        entry.setCreatedAt(createdAt);

        assertEquals(2L, entry.getId());
        assertEquals(200L, entry.getTransferId());
        assertEquals(20L, entry.getUserId());
        assertEquals(getLedgerType(), entry.getType());
        assertEquals(new BigDecimal("750.00"), entry.getAmount());
        assertEquals(new BigDecimal("2000.00"), entry.getBalanceBefore());
        assertEquals(new BigDecimal("1250.00"), entry.getBalanceAfter());
        assertEquals("REF-002", entry.getReferenceId());
        assertEquals(createdAt, entry.getCreatedAt());
    }

    // ==========================================
    // BUILDER
    // ==========================================

    @Test
    void testBuilder_AllFields() {
        LedgerEntry entry = createLedgerEntry();

        assertNotNull(entry);
        assertEquals(1L, entry.getId());
        assertEquals(100L, entry.getTransferId());
        assertEquals(10L, entry.getUserId());
        assertEquals(getLedgerType(), entry.getType());
        assertEquals(new BigDecimal("500.00"), entry.getAmount());
        assertEquals(new BigDecimal("1000.00"), entry.getBalanceBefore());
        assertEquals(new BigDecimal("500.00"), entry.getBalanceAfter());
        assertEquals("REF-001", entry.getReferenceId());
        assertEquals(
                LocalDateTime.of(2026, 11, 3, 10, 0),
                entry.getCreatedAt()
        );
    }

    @Test
    void testBuilder_NullFields() {
        LedgerEntry entry = LedgerEntry.builder()
                .id(null)
                .transferId(null)
                .userId(null)
                .type(null)
                .amount(null)
                .balanceBefore(null)
                .balanceAfter(null)
                .referenceId(null)
                .createdAt(null)
                .build();

        assertNull(entry.getId());
        assertNull(entry.getTransferId());
        assertNull(entry.getUserId());
        assertNull(entry.getType());
        assertNull(entry.getAmount());
        assertNull(entry.getBalanceBefore());
        assertNull(entry.getBalanceAfter());
        assertNull(entry.getReferenceId());
        assertNull(entry.getCreatedAt());
    }

    // ==========================================
    // EQUALS
    // ==========================================

    @Test
    void testEquals_SameObject() {
        LedgerEntry entry = createLedgerEntry();

        assertEquals(entry, entry);
    }

    @Test
    void testEquals_NullObject() {
        LedgerEntry entry = createLedgerEntry();

        assertNotEquals(null, entry);
    }

    @Test
    void testEquals_DifferentClass() {
        LedgerEntry entry = createLedgerEntry();

        assertNotEquals(entry, "LedgerEntry");
    }

    @Test
    void testEquals_EqualObjects() {
        LedgerEntry first = createLedgerEntry();
        LedgerEntry second = createLedgerEntry();

        assertEquals(first, second);
    }

    @Test
    void testEquals_BothObjectsWithNullFields() {
        LedgerEntry first = new LedgerEntry();
        LedgerEntry second = new LedgerEntry();

        assertEquals(first, second);
    }

    @Test
    void testEquals_DifferentValuesForEveryField() {
        LedgerEntry original = createLedgerEntry();

        assertNotEqual(original, entry -> entry.setId(2L));
        assertNotEqual(original, entry -> entry.setTransferId(200L));
        assertNotEqual(original, entry -> entry.setUserId(20L));
        assertNotEqual(original, entry -> entry.setType(null));
        assertNotEqual(original, entry ->
                entry.setAmount(new BigDecimal("600.00")));
        assertNotEqual(original, entry ->
                entry.setBalanceBefore(new BigDecimal("1100.00")));
        assertNotEqual(original, entry ->
                entry.setBalanceAfter(new BigDecimal("600.00")));
        assertNotEqual(original, entry ->
                entry.setReferenceId("REF-002"));
        assertNotEqual(original, entry ->
                entry.setCreatedAt(
                        LocalDateTime.of(2026, 11, 4, 10, 0)
                ));
    }

    private void assertNotEqual(
            LedgerEntry original,
            Consumer<LedgerEntry> change) {

        LedgerEntry modified = createLedgerEntry();
        change.accept(modified);

        assertNotEquals(original, modified);
    }

    @Test
    void testEquals_NullFieldVersusNonNullField() {
        LedgerEntry first = new LedgerEntry();
        LedgerEntry second = new LedgerEntry();

        second.setReferenceId("REF-001");
        assertNotEquals(first, second);

        first.setReferenceId("REF-001");
        assertEquals(first, second);
    }

    @Test
    void testCanEqual_WithLedgerEntry() {
        LedgerEntry entry = createLedgerEntry();

        assertTrue(entry.canEqual(new LedgerEntry()));
    }

    @Test
    void testCanEqual_WithDifferentClass() {
        LedgerEntry entry = createLedgerEntry();

        assertFalse(entry.canEqual("Not a LedgerEntry"));
    }

    @Test
    void testEquals_WhenOtherObjectRejectsCanEqual() {
        LedgerEntry entry = createLedgerEntry();

        LedgerEntry other = new LedgerEntry() {
            @Override
            public boolean canEqual(Object object) {
                return false;
            }
        };

        assertNotEquals(entry, other);
    }

    // ==========================================
    // HASH CODE
    // ==========================================

    @Test
    void testHashCode_EqualObjects() {
        LedgerEntry first = createLedgerEntry();
        LedgerEntry second = createLedgerEntry();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHashCode_NullFields() {
        LedgerEntry entry = new LedgerEntry();

        assertDoesNotThrow(entry::hashCode);
    }

    @Test
    void testHashCode_NonNullFields() {
        LedgerEntry entry = createLedgerEntry();

        assertDoesNotThrow(entry::hashCode);
    }

    // ==========================================
    // TO STRING
    // ==========================================

    @Test
    void testToString_AllFields() {
        LedgerEntry entry = createLedgerEntry();

        String result = entry.toString();

        assertNotNull(result);
        assertTrue(result.contains("LedgerEntry"));
        assertTrue(result.contains("id="));
        assertTrue(result.contains("transferId="));
        assertTrue(result.contains("userId="));
        assertTrue(result.contains("type="));
        assertTrue(result.contains("amount="));
        assertTrue(result.contains("balanceBefore="));
        assertTrue(result.contains("balanceAfter="));
        assertTrue(result.contains("referenceId="));
        assertTrue(result.contains("createdAt="));
    }

    @Test
    void testToString_NullFields() {
        LedgerEntry entry = new LedgerEntry();

        String result = entry.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=null"));
        assertTrue(result.contains("transferId=null"));
        assertTrue(result.contains("userId=null"));
        assertTrue(result.contains("type=null"));
        assertTrue(result.contains("amount=null"));
        assertTrue(result.contains("balanceBefore=null"));
        assertTrue(result.contains("balanceAfter=null"));
        assertTrue(result.contains("referenceId=null"));
        assertTrue(result.contains("createdAt=null"));
    }
}
