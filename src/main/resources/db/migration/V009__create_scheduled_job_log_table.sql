-- Create scheduled_job_log table
CREATE TABLE scheduled_job_log (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    execution_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    batch_id VARCHAR(255)
);

-- Create indexes for job log queries
CREATE INDEX idx_scheduled_job_log_job_name ON scheduled_job_log(job_name);
CREATE INDEX idx_scheduled_job_log_execution_time ON scheduled_job_log(execution_time);
CREATE INDEX idx_scheduled_job_log_batch_id ON scheduled_job_log(batch_id);
