package com.p2pledger.repository;

import com.p2pledger.model.ScheduledJobLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, Long> {

    List<ScheduledJobLog> findByJobNameOrderByExecutionTimeDesc(String jobName);
}
