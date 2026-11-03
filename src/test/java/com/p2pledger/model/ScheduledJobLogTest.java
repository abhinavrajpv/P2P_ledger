package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ScheduledJobLogTest {

    private ScheduledJobStatus getStatus() {
        return ScheduledJobStatus.values()[0];
    }

    private ScheduledJobLog createScheduledJobLog() {
        return ScheduledJobLog.builder()
                .id(1L)
                .jobName("SettlementJob")
                .executionTime(LocalDateTime.of(2026, 10, 1, 10, 0))
                .status(getStatus())
                .details("Settlement job completed")
                .batchId("BATCH-001")
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        ScheduledJobLog log = new ScheduledJobLog();

        assertNull(log.getId());
        assertNull(log.getJobName());
        assertNull(log.getExecutionTime());
        assertNull(log.getStatus());
        assertNull(log.getDetails());
        assertNull(log.getBatchId());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime executionTime =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        ScheduledJobLog log = new ScheduledJobLog(
                1L,
                "SettlementJob",
                executionTime,
                getStatus(),
                "Settlement job completed",
                "BATCH-001"
        );

        assertEquals(1L, log.getId());
        assertEquals("SettlementJob", log.getJobName());
        assertEquals(executionTime, log.getExecutionTime());
        assertEquals(getStatus(), log.getStatus());
        assertEquals("Settlement job completed", log.getDetails());
        assertEquals("BATCH-001", log.getBatchId());
    }

    @Test
    void gettersAndSettersTest() {
        ScheduledJobLog log = new ScheduledJobLog();

        LocalDateTime executionTime =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        log.setId(1L);
        log.setJobName("SettlementJob");
        log.setExecutionTime(executionTime);
        log.setStatus(getStatus());
        log.setDetails("Settlement job completed");
        log.setBatchId("BATCH-001");

        assertEquals(1L, log.getId());
        assertEquals("SettlementJob", log.getJobName());
        assertEquals(executionTime, log.getExecutionTime());
        assertEquals(getStatus(), log.getStatus());
        assertEquals("Settlement job completed", log.getDetails());
        assertEquals("BATCH-001", log.getBatchId());
    }

    @Test
    void builderTest() {
        ScheduledJobLog log = createScheduledJobLog();

        assertEquals(1L, log.getId());
        assertEquals("SettlementJob", log.getJobName());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 10, 0),
                log.getExecutionTime()
        );
        assertEquals(getStatus(), log.getStatus());
        assertEquals("Settlement job completed", log.getDetails());
        assertEquals("BATCH-001", log.getBatchId());
    }

    @Test
    void builderWithNullBatchIdTest() {
        ScheduledJobLog log = ScheduledJobLog.builder()
                .id(2L)
                .jobName("DailySummaryJob")
                .executionTime(LocalDateTime.of(2026, 10, 2, 10, 0))
                .status(getStatus())
                .details("Daily summary generated")
                .batchId(null)
                .build();

        assertNull(log.getBatchId());
    }

    @Test
    void equalsSameObjectTest() {
        ScheduledJobLog log = createScheduledJobLog();

        assertEquals(log, log);
    }

    @Test
    void equalsEqualObjectsTest() {
        ScheduledJobLog log1 = createScheduledJobLog();
        ScheduledJobLog log2 = createScheduledJobLog();

        assertEquals(log1, log2);
    }

    @Test
    void equalsDifferentObjectsTest() {
        ScheduledJobLog log1 = createScheduledJobLog();

        ScheduledJobLog log2 = ScheduledJobLog.builder()
                .id(2L)
                .jobName("DailySummaryJob")
                .executionTime(LocalDateTime.of(2026, 10, 2, 10, 0))
                .status(getStatus())
                .details("Daily summary generated")
                .batchId("BATCH-002")
                .build();

        assertNotEquals(log1, log2);
    }

    @Test
    void equalsNullTest() {
        ScheduledJobLog log = createScheduledJobLog();

        assertNotEquals(null, log);
    }

    @Test
    void canEqualTest() {
        ScheduledJobLog log = createScheduledJobLog();

        assertTrue(log.canEqual(createScheduledJobLog()));
        assertFalse(log.canEqual("not a scheduled job log"));
        assertFalse(log.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        ScheduledJobLog log = createScheduledJobLog();

        ScheduledJobLog other = new ScheduledJobLog() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(log, other);
    }

    @Test
    void hashCodeTest() {
        ScheduledJobLog log1 = createScheduledJobLog();
        ScheduledJobLog log2 = createScheduledJobLog();

        assertEquals(log1.hashCode(), log2.hashCode());
    }

    @Test
    void toStringTest() {
        ScheduledJobLog log = createScheduledJobLog();

        String result = log.toString();

        assertTrue(result.contains("SettlementJob"));
        assertTrue(result.contains("Settlement job completed"));
        assertTrue(result.contains("BATCH-001"));
    }

    @Test
    void equalsDifferentStatusTest() {
        ScheduledJobStatus[] statuses = ScheduledJobStatus.values();

        if (statuses.length > 1) {
            ScheduledJobLog log1 = createScheduledJobLog();
            ScheduledJobLog log2 = createScheduledJobLog();

            log2.setStatus(statuses[1]);

            assertNotEquals(log1, log2);
        }
    }
}