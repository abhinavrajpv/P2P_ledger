package com.p2pledger.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogTest {

    private AuditLog createAuditLog() {
        return AuditLog.builder()
                .id(1L)
                .actorUserId(10L)
                .action("CREATE")
                .entityType("TRANSFER")
                .entityId("TR-001")
                .details("Transfer created")
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();
    }

    @Test
    void noArgsConstructorTest() {
        AuditLog auditLog = new AuditLog();

        assertNull(auditLog.getId());
        assertNull(auditLog.getActorUserId());
        assertNull(auditLog.getAction());
        assertNull(auditLog.getEntityType());
        assertNull(auditLog.getEntityId());
        assertNull(auditLog.getDetails());
        assertNull(auditLog.getCreatedAt());
    }

    @Test
    void allArgsConstructorTest() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);

        AuditLog auditLog = new AuditLog(
                1L,
                10L,
                "CREATE",
                "TRANSFER",
                "TR-001",
                "Transfer created",
                createdAt
        );

        assertEquals(1L, auditLog.getId());
        assertEquals(10L, auditLog.getActorUserId());
        assertEquals("CREATE", auditLog.getAction());
        assertEquals("TRANSFER", auditLog.getEntityType());
        assertEquals("TR-001", auditLog.getEntityId());
        assertEquals("Transfer created", auditLog.getDetails());
        assertEquals(createdAt, auditLog.getCreatedAt());
    }

    @Test
    void gettersAndSettersTest() {
        AuditLog auditLog = new AuditLog();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);

        auditLog.setId(1L);
        auditLog.setActorUserId(10L);
        auditLog.setAction("UPDATE");
        auditLog.setEntityType("WALLET");
        auditLog.setEntityId("WALLET-001");
        auditLog.setDetails("Wallet updated");
        auditLog.setCreatedAt(createdAt);

        assertEquals(1L, auditLog.getId());
        assertEquals(10L, auditLog.getActorUserId());
        assertEquals("UPDATE", auditLog.getAction());
        assertEquals("WALLET", auditLog.getEntityType());
        assertEquals("WALLET-001", auditLog.getEntityId());
        assertEquals("Wallet updated", auditLog.getDetails());
        assertEquals(createdAt, auditLog.getCreatedAt());
    }

    @Test
    void builderTest() {
        AuditLog auditLog = createAuditLog();

        assertEquals(1L, auditLog.getId());
        assertEquals(10L, auditLog.getActorUserId());
        assertEquals("CREATE", auditLog.getAction());
        assertEquals("TRANSFER", auditLog.getEntityType());
        assertEquals("TR-001", auditLog.getEntityId());
        assertEquals("Transfer created", auditLog.getDetails());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 10, 0),
                auditLog.getCreatedAt()
        );
    }

    @Test
    void builderWithNullableFieldsTest() {
        AuditLog auditLog = AuditLog.builder()
                .id(2L)
                .actorUserId(null)
                .action("LOGIN")
                .entityType("USER")
                .entityId("USER-002")
                .details(null)
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        assertNull(auditLog.getActorUserId());
        assertNull(auditLog.getDetails());
    }

    @Test
    void equalsSameObjectTest() {
        AuditLog auditLog = createAuditLog();

        assertEquals(auditLog, auditLog);
    }

    @Test
    void equalsEqualObjectsTest() {
        AuditLog auditLog1 = createAuditLog();
        AuditLog auditLog2 = createAuditLog();

        assertEquals(auditLog1, auditLog2);
    }

    @Test
    void equalsDifferentObjectsTest() {
        AuditLog auditLog1 = createAuditLog();

        AuditLog auditLog2 = AuditLog.builder()
                .id(2L)
                .actorUserId(20L)
                .action("DELETE")
                .entityType("USER")
                .entityId("USER-002")
                .details("User deleted")
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        assertNotEquals(auditLog1, auditLog2);
    }

    @Test
    void equalsNullTest() {
        AuditLog auditLog = createAuditLog();

        assertNotEquals(null, auditLog);
    }

    @Test
    void canEqualTest() {
        AuditLog auditLog = createAuditLog();

        assertTrue(auditLog.canEqual(createAuditLog()));
        assertFalse(auditLog.canEqual("not an audit log"));
        assertFalse(auditLog.canEqual(null));
    }

    @Test
    void equalsCanEqualFalseTest() {
        AuditLog auditLog = createAuditLog();

        AuditLog other = new AuditLog() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };

        assertNotEquals(auditLog, other);
    }

    @Test
    void hashCodeTest() {
        AuditLog auditLog1 = createAuditLog();
        AuditLog auditLog2 = createAuditLog();

        assertEquals(auditLog1.hashCode(), auditLog2.hashCode());
    }

    @Test
    void toStringTest() {
        AuditLog auditLog = createAuditLog();

        String result = auditLog.toString();

        assertTrue(result.contains("CREATE"));
        assertTrue(result.contains("TRANSFER"));
        assertTrue(result.contains("TR-001"));
        assertTrue(result.contains("Transfer created"));
    }
}