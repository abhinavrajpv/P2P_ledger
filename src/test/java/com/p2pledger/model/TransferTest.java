
package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TransferTest {

    private TransferStatus getTransferStatus() {
        assertTrue(TransferStatus.values().length > 0);
        return TransferStatus.values()[0];
    }

    private AdminReviewStatus getAdminReviewStatus() {
        assertTrue(AdminReviewStatus.values().length > 0);
        return AdminReviewStatus.values()[0];
    }

    private Transfer createTransfer() {
        return Transfer.builder()
                .id(1L)
                .senderId(10L)
                .receiverId(20L)
                .amount(new BigDecimal("500.00"))
                .requestId("REQ-001")
                .status(getTransferStatus())
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .settlementDate(LocalDateTime.of(2026, 11, 2, 10, 0))
                .fraudFlagged(true)
                .adminReviewStatus(getAdminReviewStatus())
                .build();
    }

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    @Test
    void testNoArgsConstructor() {
        Transfer transfer = new Transfer();

        assertNotNull(transfer);
        assertNull(transfer.getId());
        assertNull(transfer.getSenderId());
        assertNull(transfer.getReceiverId());
        assertNull(transfer.getAmount());
        assertNull(transfer.getRequestId());
        assertNull(transfer.getStatus());
        assertNull(transfer.getCreatedAt());
        assertNull(transfer.getSettlementDate());
        assertFalse(transfer.isFraudFlagged());
        assertNull(transfer.getAdminReviewStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime settlementDate =
                LocalDateTime.of(2026, 11, 2, 10, 0);

        Transfer transfer = new Transfer(
                1L,
                10L,
                20L,
                new BigDecimal("500.00"),
                "REQ-001",
                getTransferStatus(),
                createdAt,
                settlementDate,
                true,
                getAdminReviewStatus()
        );

        assertEquals(1L, transfer.getId());
        assertEquals(10L, transfer.getSenderId());
        assertEquals(20L, transfer.getReceiverId());
        assertEquals(new BigDecimal("500.00"), transfer.getAmount());
        assertEquals("REQ-001", transfer.getRequestId());
        assertEquals(getTransferStatus(), transfer.getStatus());
        assertEquals(createdAt, transfer.getCreatedAt());
        assertEquals(settlementDate, transfer.getSettlementDate());
        assertTrue(transfer.isFraudFlagged());
        assertEquals(getAdminReviewStatus(), transfer.getAdminReviewStatus());
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    @Test
    void testGettersAndSetters() {
        Transfer transfer = new Transfer();

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 11, 3, 10, 0);
        LocalDateTime settlementDate =
                LocalDateTime.of(2026, 11, 4, 10, 0);

        transfer.setId(2L);
        transfer.setSenderId(30L);
        transfer.setReceiverId(40L);
        transfer.setAmount(new BigDecimal("750.00"));
        transfer.setRequestId("REQ-002");
        transfer.setStatus(getTransferStatus());
        transfer.setCreatedAt(createdAt);
        transfer.setSettlementDate(settlementDate);
        transfer.setFraudFlagged(false);
        transfer.setAdminReviewStatus(getAdminReviewStatus());

        assertEquals(2L, transfer.getId());
        assertEquals(30L, transfer.getSenderId());
        assertEquals(40L, transfer.getReceiverId());
        assertEquals(new BigDecimal("750.00"), transfer.getAmount());
        assertEquals("REQ-002", transfer.getRequestId());
        assertEquals(getTransferStatus(), transfer.getStatus());
        assertEquals(createdAt, transfer.getCreatedAt());
        assertEquals(settlementDate, transfer.getSettlementDate());
        assertFalse(transfer.isFraudFlagged());
        assertEquals(getAdminReviewStatus(), transfer.getAdminReviewStatus());
    }

    @Test
    void testSetFraudFlaggedTrueAndFalse() {
        Transfer transfer = new Transfer();

        transfer.setFraudFlagged(true);
        assertTrue(transfer.isFraudFlagged());

        transfer.setFraudFlagged(false);
        assertFalse(transfer.isFraudFlagged());
    }

    // ==========================================
    // BUILDER
    // ==========================================

    @Test
    void testBuilder_AllFields() {
        Transfer transfer = createTransfer();

        assertNotNull(transfer);
        assertEquals(1L, transfer.getId());
        assertEquals(10L, transfer.getSenderId());
        assertEquals(20L, transfer.getReceiverId());
        assertEquals(new BigDecimal("500.00"), transfer.getAmount());
        assertEquals("REQ-001", transfer.getRequestId());
        assertEquals(getTransferStatus(), transfer.getStatus());
        assertEquals(
                LocalDateTime.of(2026, 11, 1, 10, 0),
                transfer.getCreatedAt()
        );
        assertEquals(
                LocalDateTime.of(2026, 11, 2, 10, 0),
                transfer.getSettlementDate()
        );
        assertTrue(transfer.isFraudFlagged());
        assertEquals(getAdminReviewStatus(), transfer.getAdminReviewStatus());
    }

    @Test
    void testBuilder_NullFields() {
        Transfer transfer = Transfer.builder()
                .id(null)
                .senderId(null)
                .receiverId(null)
                .amount(null)
                .requestId(null)
                .status(null)
                .createdAt(null)
                .settlementDate(null)
                .adminReviewStatus(null)
                .build();

        assertNull(transfer.getId());
        assertNull(transfer.getSenderId());
        assertNull(transfer.getReceiverId());
        assertNull(transfer.getAmount());
        assertNull(transfer.getRequestId());
        assertNull(transfer.getStatus());
        assertNull(transfer.getCreatedAt());
        assertNull(transfer.getSettlementDate());
        assertFalse(transfer.isFraudFlagged());
        assertNull(transfer.getAdminReviewStatus());
    }

    // ==========================================
    // EQUALS
    // ==========================================

    @Test
    void testEquals_SameObject() {
        Transfer transfer = createTransfer();

        assertEquals(transfer, transfer);
    }

    @Test
    void testEquals_NullObject() {
        Transfer transfer = createTransfer();

        assertNotEquals(null, transfer);
    }

    @Test
    void testEquals_DifferentClass() {
        Transfer transfer = createTransfer();

        assertNotEquals(transfer, "Transfer");
    }

    @Test
    void testEquals_EqualObjects() {
        Transfer first = createTransfer();
        Transfer second = createTransfer();

        assertEquals(first, second);
    }

    @Test
    void testEquals_BothObjectsWithNullFields() {
        Transfer first = new Transfer();
        Transfer second = new Transfer();

        assertEquals(first, second);
    }

    @Test
    void testEquals_DifferentValuesForEveryField() {
        Transfer original = createTransfer();

        assertNotEqual(original, t -> t.setId(2L));
        assertNotEqual(original, t -> t.setSenderId(11L));
        assertNotEqual(original, t -> t.setReceiverId(21L));
        assertNotEqual(original, t ->
                t.setAmount(new BigDecimal("600.00")));
        assertNotEqual(original, t -> t.setRequestId("REQ-002"));

        if (TransferStatus.values().length > 1) {
            assertNotEqual(original, t ->
                    t.setStatus(TransferStatus.values()[1]));
        }

        assertNotEqual(original, t ->
                t.setCreatedAt(LocalDateTime.of(2026, 11, 3, 10, 0)));

        assertNotEqual(original, t ->
                t.setSettlementDate(LocalDateTime.of(2026, 11, 4, 10, 0)));

        assertNotEqual(original, t -> t.setFraudFlagged(false));

        if (AdminReviewStatus.values().length > 1) {
            assertNotEqual(original, t ->
                    t.setAdminReviewStatus(AdminReviewStatus.values()[1]));
        }
    }

    private void assertNotEqual(
            Transfer original,
            Consumer<Transfer> change) {

        Transfer modified = createTransfer();
        change.accept(modified);

        assertNotEquals(original, modified);
    }

    @Test
    void testEquals_NullFieldVersusNonNullField() {
        Transfer first = new Transfer();
        Transfer second = new Transfer();

        second.setRequestId("REQ-001");
        assertNotEquals(first, second);

        first.setRequestId("REQ-001");
        assertEquals(first, second);
    }

    @Test
    void testCanEqual_WithTransfer() {
        Transfer transfer = createTransfer();

        assertTrue(transfer.canEqual(new Transfer()));
    }

    @Test
    void testCanEqual_WithDifferentClass() {
        Transfer transfer = createTransfer();

        assertFalse(transfer.canEqual("Not a Transfer"));
    }

    @Test
    void testEquals_WhenOtherObjectRejectsCanEqual() {
        Transfer transfer = createTransfer();

        Transfer other = new Transfer() {
            @Override
            public boolean canEqual(Object object) {
                return false;
            }
        };

        assertNotEquals(transfer, other);
    }

    // ==========================================
    // HASH CODE
    // ==========================================

    @Test
    void testHashCode_EqualObjects() {
        Transfer first = createTransfer();
        Transfer second = createTransfer();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHashCode_NullFields() {
        Transfer transfer = new Transfer();

        assertDoesNotThrow(transfer::hashCode);
    }

    @Test
    void testHashCode_NonNullFields() {
        Transfer transfer = createTransfer();

        assertDoesNotThrow(transfer::hashCode);
    }

    // ==========================================
    // TO STRING
    // ==========================================

    @Test
    void testToString_AllFields() {
        Transfer transfer = createTransfer();

        String result = transfer.toString();

        assertNotNull(result);
        assertTrue(result.contains("Transfer"));
        assertTrue(result.contains("id="));
        assertTrue(result.contains("senderId="));
        assertTrue(result.contains("receiverId="));
        assertTrue(result.contains("amount="));
        assertTrue(result.contains("requestId="));
        assertTrue(result.contains("status="));
        assertTrue(result.contains("createdAt="));
        assertTrue(result.contains("settlementDate="));
        assertTrue(result.contains("fraudFlagged="));
        assertTrue(result.contains("adminReviewStatus="));
    }

    @Test
    void testToString_NullFields() {
        Transfer transfer = new Transfer();

        String result = transfer.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=null"));
        assertTrue(result.contains("senderId=null"));
        assertTrue(result.contains("receiverId=null"));
        assertTrue(result.contains("amount=null"));
        assertTrue(result.contains("requestId=null"));
        assertTrue(result.contains("status=null"));
        assertTrue(result.contains("createdAt=null"));
        assertTrue(result.contains("settlementDate=null"));
        assertTrue(result.contains("fraudFlagged=false"));
        assertTrue(result.contains("adminReviewStatus=null"));
    }
}
