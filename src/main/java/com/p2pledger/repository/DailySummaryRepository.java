package com.p2pledger.repository;

import com.p2pledger.model.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findBySummaryDate(LocalDate date);

    List<DailySummary> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);
}
