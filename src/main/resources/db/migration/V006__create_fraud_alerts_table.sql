-- Create fraud_alerts table
CREATE TABLE fraud_alerts (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL REFERENCES transfers(id) ON DELETE CASCADE,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP WITHOUT TIME ZONE
);

-- Create indexes for fraud alert queries
CREATE INDEX idx_fraud_alerts_transfer_id ON fraud_alerts(transfer_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_reviewed_by ON fraud_alerts(reviewed_by);
