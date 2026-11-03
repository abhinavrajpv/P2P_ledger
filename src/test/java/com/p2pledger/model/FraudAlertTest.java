package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class FraudAlertTest {

    private FraudStatus getStatus() {
        return FraudStatus.values()[0];
    }

    private FraudAlert createFraudAlert() {
        return FraudAlert.builder()
                .id(1L)
                .transferId(100L)
                .reason("Unusual transaction amount")
                .status(getStatus())
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .reviewedBy(20L)
                .reviewedAt(LocalDateTime.of(2026, 10, 1, 11, 0))
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        FraudAlert alert = new FraudAlert();

        assertNull(alert.getId());
        assertNull(alert.getTransferId());
        assertNull(alert.getReason());
        assertNull(alert.getStatus());
        assertNull(alert.getCreatedAt());
        assertNull(alert.getReviewedBy());
        assertNull(alert.getReviewedAt());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 10, 1, 11, 0);

        FraudAlert alert = new FraudAlert(
                1L,
                100L,
                "Unusual transaction amount",
                getStatus(),
                createdAt,
                20L,
                reviewedAt
        );

        assertEquals(1L, alert.getId());
        assertEquals(100L, alert.getTransferId());
        assertEquals("Unusual transaction amount", alert.getReason());
        assertEquals(getStatus(), alert.getStatus());
        assertEquals(createdAt, alert.getCreatedAt());
        assertEquals(20L, alert.getReviewedBy());
        assertEquals(reviewedAt, alert.getReviewedAt());
    }

    @Test
    void gettersAndSettersTest() {
        FraudAlert alert = new FraudAlert();

        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 10, 1, 11, 0);

        alert.setId(1L);
        alert.setTransferId(100L);
        alert.setReason("Unusual transaction amount");
        alert.setStatus(getStatus());
        alert.setCreatedAt(createdAt);
        alert.setReviewedBy(20L);
        alert.setReviewedAt(reviewedAt);

        assertEquals(1L, alert.getId());
        assertEquals(100L, alert.getTransferId());
        assertEquals("Unusual transaction amount", alert.getReason());
        assertEquals(getStatus(), alert.getStatus());
        assertEquals(createdAt, alert.getCreatedAt());
        assertEquals(20L, alert.getReviewedBy());
        assertEquals(reviewedAt, alert.getReviewedAt());
    }

    @Test
    void builderTest() {
        FraudAlert alert = createFraudAlert();

        assertEquals(1L, alert.getId());
        assertEquals(100L, alert.getTransferId());
        assertEquals("Unusual transaction amount", alert.getReason());
        assertEquals(getStatus(), alert.getStatus());
        assertEquals(20L, alert.getReviewedBy());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 10, 0),
                alert.getCreatedAt()
        );
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 11, 0),
                alert.getReviewedAt()
        );
    }

    @Test
    void builderWithNullReviewFieldsTest() {
        FraudAlert alert = FraudAlert.builder()
                .id(2L)
                .transferId(200L)
                .reason("Suspicious activity")
                .status(getStatus())
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .reviewedBy(null)
                .reviewedAt(null)
                .build();

        assertNull(alert.getReviewedBy());
        assertNull(alert.getReviewedAt());
    }

    @Test
    void equalsSameObjectTest() {
        FraudAlert alert = createFraudAlert();

        assertEquals(alert, alert);
    }

    @Test
    void equalsEqualObjectsTest() {
        FraudAlert alert1 = createFraudAlert();
        FraudAlert alert2 = createFraudAlert();

        assertEquals(alert1, alert2);
    }

    @Test
    void equalsDifferentObjectsTest() {
        FraudAlert alert1 = createFraudAlert();

        FraudAlert alert2 = FraudAlert.builder()
                .id(2L)
                .transferId(200L)
                .reason("Different reason")
                .status(getStatus())
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .reviewedBy(30L)
                .reviewedAt(LocalDateTime.of(2026, 10, 2, 11, 0))
                .build();

        assertNotEquals(alert1, alert2);
    }

    @Test
    void equalsNullTest() {
        FraudAlert alert = createFraudAlert();

        assertNotEquals(null, alert);
    }

    @Test
    void canEqualTest() {
        FraudAlert alert = createFraudAlert();

        assertTrue(alert.canEqual(createFraudAlert()));
        assertFalse(alert.canEqual("not a fraud alert"));
        assertFalse(alert.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        FraudAlert alert = createFraudAlert();

        FraudAlert other = new FraudAlert() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(alert, other);
    }

    @Test
    void hashCodeTest() {
        FraudAlert alert1 = createFraudAlert();
        FraudAlert alert2 = createFraudAlert();

        assertEquals(alert1.hashCode(), alert2.hashCode());
    }

    @Test
    void toStringTest() {
        FraudAlert alert = createFraudAlert();

        String result = alert.toString();

        assertTrue(result.contains("Unusual transaction amount"));
        assertTrue(result.contains("transferId=100"));
        assertTrue(result.contains("reviewedBy=20"));
    }

    @Test
    void equalsDifferentStatusTest() {
        FraudStatus[] statuses = FraudStatus.values();

        if (statuses.length > 1) {
            FraudAlert alert1 = createFraudAlert();
            FraudAlert alert2 = createFraudAlert();

            alert2.setStatus(statuses[1]);

            assertNotEquals(alert1, alert2);
        }
    }
}