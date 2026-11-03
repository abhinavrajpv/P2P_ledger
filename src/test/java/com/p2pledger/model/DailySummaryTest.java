
package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class DailySummaryTest {

    private DailySummary createDailySummary() {
        return DailySummary.builder()
                .id(1L)
                .summaryDate(LocalDate.of(2026, 11, 3))
                .totalTransfersInitiated(100L)
                .totalAmountInitiated(new BigDecimal("10000.00"))
                .totalTransfersSettled(80L)
                .totalAmountSettled(new BigDecimal("8000.00"))
                .fraudAlertsRaised(5L)
                .newUserRegistrations(10L)
                .pendingSettlementCount(20L)
                .pendingSettlementAmount(new BigDecimal("2000.00"))
                .totalDebits(new BigDecimal("5000.00"))
                .totalCredits(new BigDecimal("5000.00"))
                .debitCount(50L)
                .creditCount(50L)
                .reconciliationBalanced(true)
                .reconciliationMismatch(BigDecimal.ZERO)
                .createdAt(LocalDateTime.of(2026, 11, 3, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 3, 11, 0))
                .build();
    }

    // ==========================================
    // NO-ARGUMENT CONSTRUCTOR
    // ==========================================

    @Test
    void testNoArgsConstructor() {
        DailySummary summary = new DailySummary();

        assertNotNull(summary);
        assertNull(summary.getId());
        assertNull(summary.getSummaryDate());
        assertNull(summary.getTotalTransfersInitiated());
        assertNull(summary.getTotalAmountInitiated());
        assertNull(summary.getTotalTransfersSettled());
        assertNull(summary.getTotalAmountSettled());
        assertNull(summary.getFraudAlertsRaised());
        assertNull(summary.getNewUserRegistrations());
        assertNull(summary.getPendingSettlementCount());
        assertNull(summary.getPendingSettlementAmount());
        assertNull(summary.getTotalDebits());
        assertNull(summary.getTotalCredits());
        assertNull(summary.getDebitCount());
        assertNull(summary.getCreditCount());
        assertNull(summary.getReconciliationBalanced());
        assertNull(summary.getReconciliationMismatch());
        assertNull(summary.getCreatedAt());
        assertNull(summary.getUpdatedAt());
    }

    // ==========================================
    // ALL-ARGUMENT CONSTRUCTOR
    // ==========================================

    @Test
    void testAllArgsConstructor() {
        DailySummary summary = new DailySummary(
                1L,
                LocalDate.of(2026, 11, 3),
                100L,
                new BigDecimal("10000.00"),
                80L,
                new BigDecimal("8000.00"),
                5L,
                10L,
                20L,
                new BigDecimal("2000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                50L,
                50L,
                true,
                BigDecimal.ZERO,
                LocalDateTime.of(2026, 11, 3, 10, 0),
                LocalDateTime.of(2026, 11, 3, 11, 0)
        );

        assertEquals(1L, summary.getId());
        assertEquals(LocalDate.of(2026, 11, 3), summary.getSummaryDate());
        assertEquals(100L, summary.getTotalTransfersInitiated());
        assertEquals(new BigDecimal("10000.00"), summary.getTotalAmountInitiated());
        assertEquals(80L, summary.getTotalTransfersSettled());
        assertEquals(new BigDecimal("8000.00"), summary.getTotalAmountSettled());
        assertEquals(5L, summary.getFraudAlertsRaised());
        assertEquals(10L, summary.getNewUserRegistrations());
        assertEquals(20L, summary.getPendingSettlementCount());
        assertEquals(new BigDecimal("2000.00"), summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalDebits());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalCredits());
        assertEquals(50L, summary.getDebitCount());
        assertEquals(50L, summary.getCreditCount());
        assertTrue(summary.getReconciliationBalanced());
        assertEquals(BigDecimal.ZERO, summary.getReconciliationMismatch());
        assertEquals(LocalDateTime.of(2026, 11, 3, 10, 0), summary.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 11, 3, 11, 0), summary.getUpdatedAt());
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    @Test
    void testGettersAndSetters() {
        DailySummary summary = new DailySummary();

        summary.setId(2L);
        summary.setSummaryDate(LocalDate.of(2026, 11, 4));
        summary.setTotalTransfersInitiated(200L);
        summary.setTotalAmountInitiated(new BigDecimal("20000.00"));
        summary.setTotalTransfersSettled(150L);
        summary.setTotalAmountSettled(new BigDecimal("15000.00"));
        summary.setFraudAlertsRaised(8L);
        summary.setNewUserRegistrations(15L);
        summary.setPendingSettlementCount(30L);
        summary.setPendingSettlementAmount(new BigDecimal("3000.00"));
        summary.setTotalDebits(new BigDecimal("7000.00"));
        summary.setTotalCredits(new BigDecimal("7000.00"));
        summary.setDebitCount(70L);
        summary.setCreditCount(70L);
        summary.setReconciliationBalanced(false);
        summary.setReconciliationMismatch(new BigDecimal("10.00"));
        summary.setCreatedAt(LocalDateTime.of(2026, 11, 4, 10, 0));
        summary.setUpdatedAt(LocalDateTime.of(2026, 11, 4, 12, 0));

        assertEquals(2L, summary.getId());
        assertEquals(LocalDate.of(2026, 11, 4), summary.getSummaryDate());
        assertEquals(200L, summary.getTotalTransfersInitiated());
        assertEquals(new BigDecimal("20000.00"), summary.getTotalAmountInitiated());
        assertEquals(150L, summary.getTotalTransfersSettled());
        assertEquals(new BigDecimal("15000.00"), summary.getTotalAmountSettled());
        assertEquals(8L, summary.getFraudAlertsRaised());
        assertEquals(15L, summary.getNewUserRegistrations());
        assertEquals(30L, summary.getPendingSettlementCount());
        assertEquals(new BigDecimal("3000.00"), summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("7000.00"), summary.getTotalDebits());
        assertEquals(new BigDecimal("7000.00"), summary.getTotalCredits());
        assertEquals(70L, summary.getDebitCount());
        assertEquals(70L, summary.getCreditCount());
        assertFalse(summary.getReconciliationBalanced());
        assertEquals(new BigDecimal("10.00"), summary.getReconciliationMismatch());
        assertEquals(LocalDateTime.of(2026, 11, 4, 10, 0), summary.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 11, 4, 12, 0), summary.getUpdatedAt());
    }

    // ==========================================
    // BUILDER
    // ==========================================

    @Test
    void testBuilder_AllFields() {
        DailySummary summary = createDailySummary();

        assertNotNull(summary);
        assertEquals(1L, summary.getId());
        assertEquals(LocalDate.of(2026, 11, 3), summary.getSummaryDate());
        assertEquals(100L, summary.getTotalTransfersInitiated());
        assertEquals(new BigDecimal("10000.00"), summary.getTotalAmountInitiated());
        assertEquals(80L, summary.getTotalTransfersSettled());
        assertEquals(new BigDecimal("8000.00"), summary.getTotalAmountSettled());
        assertEquals(5L, summary.getFraudAlertsRaised());
        assertEquals(10L, summary.getNewUserRegistrations());
        assertEquals(20L, summary.getPendingSettlementCount());
        assertEquals(new BigDecimal("2000.00"), summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalDebits());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalCredits());
        assertEquals(50L, summary.getDebitCount());
        assertEquals(50L, summary.getCreditCount());
        assertTrue(summary.getReconciliationBalanced());
        assertEquals(BigDecimal.ZERO, summary.getReconciliationMismatch());
        assertNotNull(summary.getCreatedAt());
        assertNotNull(summary.getUpdatedAt());
    }

    @Test
    void testBuilder_WithNullFields() {
        DailySummary summary = DailySummary.builder()
                .id(null)
                .summaryDate(null)
                .totalTransfersInitiated(null)
                .totalAmountInitiated(null)
                .totalTransfersSettled(null)
                .totalAmountSettled(null)
                .fraudAlertsRaised(null)
                .newUserRegistrations(null)
                .pendingSettlementCount(null)
                .pendingSettlementAmount(null)
                .totalDebits(null)
                .totalCredits(null)
                .debitCount(null)
                .creditCount(null)
                .reconciliationBalanced(null)
                .reconciliationMismatch(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        assertNotNull(summary);
        assertNull(summary.getId());
        assertNull(summary.getSummaryDate());
        assertNull(summary.getTotalTransfersInitiated());
        assertNull(summary.getTotalAmountInitiated());
        assertNull(summary.getTotalTransfersSettled());
        assertNull(summary.getTotalAmountSettled());
        assertNull(summary.getFraudAlertsRaised());
        assertNull(summary.getNewUserRegistrations());
        assertNull(summary.getPendingSettlementCount());
        assertNull(summary.getPendingSettlementAmount());
        assertNull(summary.getTotalDebits());
        assertNull(summary.getTotalCredits());
        assertNull(summary.getDebitCount());
        assertNull(summary.getCreditCount());
        assertNull(summary.getReconciliationBalanced());
        assertNull(summary.getReconciliationMismatch());
        assertNull(summary.getCreatedAt());
        assertNull(summary.getUpdatedAt());
    }

    // ==========================================
    // EQUALS
    // ==========================================

    @Test
    void testEquals_SameObject() {
        DailySummary summary = createDailySummary();

        assertEquals(summary, summary);
    }

    @Test
    void testEquals_NullObject() {
        DailySummary summary = createDailySummary();

        assertNotEquals(null, summary);
    }

    @Test
    void testEquals_DifferentClass() {
        DailySummary summary = createDailySummary();

        assertNotEquals(summary, "DailySummary");
    }

    @Test
    void testEquals_EqualObjects() {
        DailySummary first = createDailySummary();
        DailySummary second = createDailySummary();

        assertEquals(first, second);
    }

    @Test
    void testEquals_BothObjectsWithNullFields() {
        DailySummary first = new DailySummary();
        DailySummary second = new DailySummary();

        assertEquals(first, second);
    }

    @Test
    void testEquals_DifferentValuesForEveryField() {
        DailySummary first = createDailySummary();

        assertNotEqual(first, s -> s.setId(2L));
        assertNotEqual(first, s -> s.setSummaryDate(LocalDate.of(2026, 11, 4)));
        assertNotEqual(first, s -> s.setTotalTransfersInitiated(101L));
        assertNotEqual(first, s -> s.setTotalAmountInitiated(new BigDecimal("10001.00")));
        assertNotEqual(first, s -> s.setTotalTransfersSettled(81L));
        assertNotEqual(first, s -> s.setTotalAmountSettled(new BigDecimal("8001.00")));
        assertNotEqual(first, s -> s.setFraudAlertsRaised(6L));
        assertNotEqual(first, s -> s.setNewUserRegistrations(11L));
        assertNotEqual(first, s -> s.setPendingSettlementCount(21L));
        assertNotEqual(first, s -> s.setPendingSettlementAmount(new BigDecimal("2001.00")));
        assertNotEqual(first, s -> s.setTotalDebits(new BigDecimal("5001.00")));
        assertNotEqual(first, s -> s.setTotalCredits(new BigDecimal("5001.00")));
        assertNotEqual(first, s -> s.setDebitCount(51L));
        assertNotEqual(first, s -> s.setCreditCount(51L));
        assertNotEqual(first, s -> s.setReconciliationBalanced(false));
        assertNotEqual(first, s -> s.setReconciliationMismatch(new BigDecimal("1.00")));
        assertNotEqual(first, s -> s.setCreatedAt(LocalDateTime.of(2026, 11, 3, 12, 0)));
        assertNotEqual(first, s -> s.setUpdatedAt(LocalDateTime.of(2026, 11, 3, 12, 0)));
    }

    private void assertNotEqual(
            DailySummary original,
            java.util.function.Consumer<DailySummary> change) {

        DailySummary modified = createDailySummary();
        change.accept(modified);

        assertNotEquals(original, modified);
    }

    @Test
    void testCanEqual_WithDailySummary() {
        DailySummary summary = createDailySummary();

        assertTrue(summary.canEqual(new DailySummary()));
    }

    @Test
    void testCanEqual_WithDifferentClass() {
        DailySummary summary = createDailySummary();

        assertFalse(summary.canEqual("Not a DailySummary"));
    }

    @Test
    void testEquals_WhenOtherObjectRejectsCanEqual() {
        DailySummary summary = createDailySummary();

        DailySummary other = new DailySummary() {
            @Override
            public boolean canEqual(Object object) {
                return false;
            }
        };

        assertNotEquals(summary, other);
    }

    // ==========================================
    // HASH CODE
    // ==========================================

    @Test
    void testHashCode_EqualObjects() {
        DailySummary first = createDailySummary();
        DailySummary second = createDailySummary();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHashCode_NullFields() {
        DailySummary summary = new DailySummary();

        assertDoesNotThrow(summary::hashCode);
    }

    @Test
    void testHashCode_NonNullFields() {
        DailySummary summary = createDailySummary();

        assertDoesNotThrow(summary::hashCode);
    }

    // ==========================================
    // TO STRING
    // ==========================================

    @Test
    void testToString() {
        DailySummary summary = createDailySummary();

        String result = summary.toString();

        assertNotNull(result);
        assertTrue(result.contains("DailySummary"));
        assertTrue(result.contains("summaryDate"));
        assertTrue(result.contains("totalTransfersInitiated"));
        assertTrue(result.contains("totalAmountInitiated"));
        assertTrue(result.contains("totalTransfersSettled"));
        assertTrue(result.contains("totalAmountSettled"));
        assertTrue(result.contains("fraudAlertsRaised"));
        assertTrue(result.contains("newUserRegistrations"));
        assertTrue(result.contains("pendingSettlementCount"));
        assertTrue(result.contains("pendingSettlementAmount"));
        assertTrue(result.contains("totalDebits"));
        assertTrue(result.contains("totalCredits"));
        assertTrue(result.contains("debitCount"));
        assertTrue(result.contains("creditCount"));
        assertTrue(result.contains("reconciliationBalanced"));
        assertTrue(result.contains("reconciliationMismatch"));
        assertTrue(result.contains("createdAt"));
        assertTrue(result.contains("updatedAt"));
    }

    @Test
    void testToString_WithNullFields() {
        DailySummary summary = new DailySummary();

        String result = summary.toString();

        assertNotNull(result);
        assertTrue(result.contains("DailySummary"));
        assertTrue(result.contains("id=null"));
        assertTrue(result.contains("summaryDate=null"));
    }
}
