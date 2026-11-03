//package com.p2pledger.model;
//
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class SettlementBatchTest {
//
//    private SettlementBatchStatus getStatus() {
//        return SettlementBatchStatus.values()[0];
//    }
//
//    private SettlementBatch createBatch() {
//        return SettlementBatch.builder()
//                .id(1L)
//                .batchId("BATCH-001")
//                .startedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
//                .completedAt(LocalDateTime.of(2026, 10, 1, 11, 0))
//                .status(getStatus())
//                .processedCount(100)
//                .failureCount(5)
//                .details("Settlement completed")
//                .build();
//    }
//
//    @Test
//    void noArgsConstructorTest() {
//        SettlementBatch batch = new SettlementBatch();
//
//        assertNull(batch.getId());
//        assertNull(batch.getBatchId());
//        assertNull(batch.getStartedAt());
//        assertNull(batch.getCompletedAt());
//        assertNull(batch.getStatus());
//        assertNull(batch.getProcessedCount());
//        assertNull(batch.getFailureCount());
//        assertNull(batch.getDetails());
//    }
//
//    @Test
//    void allArgsConstructorTest() {
//        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
//        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 11, 0);
//
//        SettlementBatch batch = new SettlementBatch(
//                1L,
//                "BATCH-001",
//                startedAt,
//                completedAt,
//                getStatus(),
//                100,
//                5,
//                "Settlement completed"
//        );
//
//        assertEquals(1L, batch.getId());
//        assertEquals("BATCH-001", batch.getBatchId());
//        assertEquals(startedAt, batch.getStartedAt());
//        assertEquals(completedAt, batch.getCompletedAt());
//        assertEquals(getStatus(), batch.getStatus());
//        assertEquals(100, batch.getProcessedCount());
//        assertEquals(5, batch.getFailureCount());
//        assertEquals("Settlement completed", batch.getDetails());
//    }
//
//    @Test
//    void gettersAndSettersTest() {
//        SettlementBatch batch = new SettlementBatch();
//
//        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
//        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 11, 0);
//
//        batch.setId(1L);
//        batch.setBatchId("BATCH-001");
//        batch.setStartedAt(startedAt);
//        batch.setCompletedAt(completedAt);
//        batch.setStatus(getStatus());
//        batch.setProcessedCount(100);
//        batch.setFailureCount(5);
//        batch.setDetails("Settlement completed");
//
//        assertEquals(1L, batch.getId());
//        assertEquals("BATCH-001", batch.getBatchId());
//        assertEquals(startedAt, batch.getStartedAt());
//        assertEquals(completedAt, batch.getCompletedAt());
//        assertEquals(getStatus(), batch.getStatus());
//        assertEquals(100, batch.getProcessedCount());
//        assertEquals(5, batch.getFailureCount());
//        assertEquals("Settlement completed", batch.getDetails());
//    }
//
//    @Test
//    void builderTest() {
//        SettlementBatch batch = createBatch();
//
//        assertEquals(1L, batch.getId());
//        assertEquals("BATCH-001", batch.getBatchId());
//        assertEquals(100, batch.getProcessedCount());
//        assertEquals(5, batch.getFailureCount());
//        assertEquals("Settlement completed", batch.getDetails());
//    }
//
//    @Test
//    void builderWithNullOptionalFieldsTest() {
//        SettlementBatch batch = SettlementBatch.builder()
//                .id(2L)
//                .batchId("BATCH-002")
//                .startedAt(LocalDateTime.of(2026, 10, 2, 10, 0))
//                .completedAt(null)
//                .status(getStatus())
//                .processedCount(50)
//                .failureCount(0)
//                .details(null)
//                .build();
//
//        assertNull(batch.getCompletedAt());
//        assertNull(batch.getDetails());
//    }
//
//    @Test
//    void equalsSameObjectTest() {
//        SettlementBatch batch = createBatch();
//
//        assertEquals(batch, batch);
//    }
//
//    @Test
//    void equalsEqualObjectsTest() {
//        SettlementBatch batch1 = createBatch();
//        SettlementBatch batch2 = createBatch();
//
//        assertEquals(batch1, batch2);
//    }
//
//    @Test
//    void equalsDifferentObjectTest() {
//        SettlementBatch batch1 = createBatch();
//
//        SettlementBatch batch2 = SettlementBatch.builder()
//                .id(2L)
//                .batchId("BATCH-002")
//                .startedAt(LocalDateTime.of(2026, 10, 2, 10, 0))
//                .completedAt(LocalDateTime.of(2026, 10, 2, 11, 0))
//                .status(getStatus())
//                .processedCount(50)
//                .failureCount(2)
//                .details("Different batch")
//                .build();
//
//        assertNotEquals(batch1, batch2);
//    }
//
//    @Test
//    void equalsNullTest() {
//        SettlementBatch batch = createBatch();
//
//        assertNotEquals(null, batch);
//    }
//
//    @Test
//    void canEqualTest() {
//        SettlementBatch batch = createBatch();
//
//        assertTrue(batch.canEqual(createBatch()));
//        assertFalse(batch.canEqual("not a settlement batch"));
//        assertFalse(batch.canEqual(null));
//    }
//
//    @Test
//    void equalsCanEqualFalseTest() {
//        SettlementBatch batch = createBatch();
//
//        SettlementBatch other = new SettlementBatch() {
//            @Override
//            public boolean canEqual(Object other) {
//                return false;
//            }
//        };
//
//        assertNotEquals(batch, other);
//    }
//
//    @Test
//    void hashCodeTest() {
//        SettlementBatch batch1 = createBatch();
//        SettlementBatch batch2 = createBatch();
//
//        assertEquals(batch1.hashCode(), batch2.hashCode());
//    }
//
//    @Test
//    void toStringTest() {
//        SettlementBatch batch = createBatch();
//
//        String result = batch.toString();
//
//        assertTrue(result.contains("BATCH-001"));
//        assertTrue(result.contains("processedCount=100"));
//        assertTrue(result.contains("failureCount=5"));
//        assertTrue(result.contains("Settlement completed"));
//    }
//
//    @Test
//    void equalsDifferentStatusTest() {
//        SettlementBatchStatus[] statuses = SettlementBatchStatus.values();
//
//        if (statuses.length > 1) {
//            SettlementBatch batch1 = createBatch();
//            SettlementBatch batch2 = createBatch();
//            batch2.setStatus(statuses[1]);
//
//            assertNotEquals(batch1, batch2);
//        }
//    }
//}

package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SettlementBatchTest {

    private SettlementBatchStatus getStatus() {
        return SettlementBatchStatus.values()[0];
    }

    private SettlementBatch createBatch() {
        return SettlementBatch.builder()
                .id(1L)
                .batchId("BATCH-001")
                .startedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .completedAt(LocalDateTime.of(2026, 10, 1, 11, 0))
                .status(getStatus())
                .processedCount(100)
                .failureCount(5)
                .details("Settlement completed")
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        SettlementBatch batch = new SettlementBatch();

        assertNull(batch.getId());
        assertNull(batch.getBatchId());
        assertNull(batch.getStartedAt());
        assertNull(batch.getCompletedAt());
        assertNull(batch.getStatus());
        assertNull(batch.getProcessedCount());
        assertNull(batch.getFailureCount());
        assertNull(batch.getDetails());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 11, 0);

        SettlementBatch batch = new SettlementBatch(
                1L,
                "BATCH-001",
                startedAt,
                completedAt,
                getStatus(),
                100,
                5,
                "Settlement completed"
        );

        assertEquals(1L, batch.getId());
        assertEquals("BATCH-001", batch.getBatchId());
        assertEquals(startedAt, batch.getStartedAt());
        assertEquals(completedAt, batch.getCompletedAt());
        assertEquals(getStatus(), batch.getStatus());
        assertEquals(100, batch.getProcessedCount());
        assertEquals(5, batch.getFailureCount());
        assertEquals("Settlement completed", batch.getDetails());
    }

    @Test
    void gettersAndSettersTest() {
        SettlementBatch batch = new SettlementBatch();

        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 11, 0);

        batch.setId(1L);
        batch.setBatchId("BATCH-001");
        batch.setStartedAt(startedAt);
        batch.setCompletedAt(completedAt);
        batch.setStatus(getStatus());
        batch.setProcessedCount(100);
        batch.setFailureCount(5);
        batch.setDetails("Settlement completed");

        assertEquals(1L, batch.getId());
        assertEquals("BATCH-001", batch.getBatchId());
        assertEquals(startedAt, batch.getStartedAt());
        assertEquals(completedAt, batch.getCompletedAt());
        assertEquals(getStatus(), batch.getStatus());
        assertEquals(100, batch.getProcessedCount());
        assertEquals(5, batch.getFailureCount());
        assertEquals("Settlement completed", batch.getDetails());
    }

    @Test
    void builderTest() {
        SettlementBatch batch = createBatch();

        assertEquals(1L, batch.getId());
        assertEquals("BATCH-001", batch.getBatchId());
        assertEquals(100, batch.getProcessedCount());
        assertEquals(5, batch.getFailureCount());
        assertEquals("Settlement completed", batch.getDetails());
    }

    /*
     * Exercises the null, equal and unequal comparison branches
     * for each individual field generated by Lombok equals().
     */
    private void exerciseField(
            Consumer<SettlementBatch> setNull,
            Consumer<SettlementBatch> setDifferentValue) {

        // Both values null: null comparison returns false.
        SettlementBatch first = createBatch();
        SettlementBatch second = createBatch();

        setNull.accept(first);
        setNull.accept(second);

        assertEquals(first, second);

        // First value null, second value non-null.
        first = createBatch();
        second = createBatch();

        setNull.accept(first);

        assertNotEquals(first, second);

        // Both values non-null but different.
        first = createBatch();
        second = createBatch();

        setDifferentValue.accept(second);

        assertNotEquals(first, second);

        // Equal non-null values.
        first = createBatch();
        second = createBatch();

        assertEquals(first, second);
    }

    @Test
    void equalsAllFieldBranchesTest() {

        exerciseField(
                batch -> batch.setId(null),
                batch -> batch.setId(2L)
        );

        exerciseField(
                batch -> batch.setBatchId(null),
                batch -> batch.setBatchId("BATCH-002")
        );

        exerciseField(
                batch -> batch.setStartedAt(null),
                batch -> batch.setStartedAt(
                        LocalDateTime.of(2026, 10, 2, 10, 0)
                )
        );

        exerciseField(
                batch -> batch.setCompletedAt(null),
                batch -> batch.setCompletedAt(
                        LocalDateTime.of(2026, 10, 2, 11, 0)
                )
        );

        exerciseField(
                batch -> batch.setStatus(null),
                batch -> {
                    SettlementBatchStatus[] statuses =
                            SettlementBatchStatus.values();

                    if (statuses.length > 1) {
                        batch.setStatus(statuses[1]);
                    }
                }
        );

        exerciseField(
                batch -> batch.setProcessedCount(null),
                batch -> batch.setProcessedCount(200)
        );

        exerciseField(
                batch -> batch.setFailureCount(null),
                batch -> batch.setFailureCount(10)
        );

        exerciseField(
                batch -> batch.setDetails(null),
                batch -> batch.setDetails("Different details")
        );
    }

    @Test
    void equalsSameObjectTest() {
        SettlementBatch batch = createBatch();

        assertEquals(batch, batch);
    }

    @Test
    void equalsEqualObjectsTest() {
        assertEquals(createBatch(), createBatch());
    }

    @Test
    void equalsNullTest() {
        SettlementBatch batch = createBatch();

        assertNotEquals(null, batch);
    }

    @Test
    void equalsDifferentTypeTest() {
        SettlementBatch batch = createBatch();

        assertNotEquals(batch, "not a settlement batch");
    }

    @Test
    void canEqualTest() {
        SettlementBatch batch = createBatch();

        assertTrue(batch.canEqual(createBatch()));
        assertFalse(batch.canEqual("not a settlement batch"));
        assertFalse(batch.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        SettlementBatch batch = createBatch();

        SettlementBatch other = new SettlementBatch() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(batch, other);
    }

    @Test
    void hashCodeNullFieldsTest() {
        SettlementBatch batch = new SettlementBatch();

        // All eight reference fields are null.
        assertDoesNotThrow(batch::hashCode);
    }

    @Test
    void hashCodeNonNullFieldsTest() {
        SettlementBatch batch = createBatch();

        // All eight reference fields are non-null.
        assertDoesNotThrow(batch::hashCode);
    }

    @Test
    void hashCodeEqualObjectsTest() {
        SettlementBatch first = createBatch();
        SettlementBatch second = createBatch();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void toStringTest() {
        SettlementBatch batch = createBatch();

        String result = batch.toString();

        assertTrue(result.contains("BATCH-001"));
        assertTrue(result.contains("processedCount=100"));
        assertTrue(result.contains("failureCount=5"));
        assertTrue(result.contains("Settlement completed"));
    }
}