package com.p2pledger.repository;

import com.p2pledger.model.ScheduledJobLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, Long> {

    List<ScheduledJobLog> findByJobNameOrderByExecutionTimeDesc(String jobName);
    
    @Query("SELECT j FROM ScheduledJobLog j ORDER BY j.executionTime DESC")
    List<ScheduledJobLog> findTop20ByOrderByExecutionTimeDesc(Pageable pageable);
    
    @Query("SELECT j FROM ScheduledJobLog j WHERE j.jobName = :jobName ORDER BY j.executionTime DESC")
    List<ScheduledJobLog> findTop20ByJobNameOrderByExecutionTimeDesc(String jobName, Pageable pageable);
}
