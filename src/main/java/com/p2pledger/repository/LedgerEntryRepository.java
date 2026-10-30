package com.p2pledger.repository;

import com.p2pledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransferId(Long transferId);

    List<LedgerEntry> findByUserId(Long userId);
}
