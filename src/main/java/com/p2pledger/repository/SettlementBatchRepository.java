package com.p2pledger.repository;

import com.p2pledger.model.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

    Optional<SettlementBatch> findByBatchId(String batchId);

    List<SettlementBatch> findByStatusOrderByStartedAtAsc(String status);

    List<SettlementBatch> findByCompletedAtIsNullOrderByStartedAtAsc();
}
