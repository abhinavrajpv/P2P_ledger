package com.p2pledger.repository;

import com.p2pledger.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Optional<FraudAlert> findByTransferId(Long transferId);

    List<FraudAlert> findByStatus(String status);

    Page<FraudAlert> findByStatus(String status, Pageable pageable);

    List<FraudAlert> findByTransferIdAndStatus(Long transferId, String status);
}
