package com.p2pledger.repository;

import com.p2pledger.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    List<AuditLog> findByEntityIdOrderByCreatedAtDesc(String entityId);

    List<AuditLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime timestamp);
}
