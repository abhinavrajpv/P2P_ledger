package com.p2pledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduled_job_log")
public class ScheduledJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledJobStatus status;

    @Column(nullable = false)
    private String details;

    @Column(name = "batch_id")
    private String batchId;
}
