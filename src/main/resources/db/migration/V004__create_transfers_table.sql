-- Create transfers table
CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    receiver_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0),
    request_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settlement_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fraud_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    admin_review_status VARCHAR(50) NOT NULL DEFAULT 'NOT_REQUIRED',
    CONSTRAINT unique_sender_request_id UNIQUE (sender_id, request_id)
);

-- Create indexes for transfer queries and duplicate detection
CREATE INDEX idx_transfers_sender_id ON transfers(sender_id);
CREATE INDEX idx_transfers_receiver_id ON transfers(receiver_id);
CREATE INDEX idx_transfers_request_id ON transfers(request_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_settlement_date ON transfers(settlement_date);
CREATE INDEX idx_transfers_fraud_flagged ON transfers(fraud_flagged);
CREATE INDEX idx_transfers_admin_review_status ON transfers(admin_review_status);

-- Composite index for duplicate detection (sender + receiver + amount + time window)
CREATE INDEX idx_transfers_sender_receiver_amount ON transfers(sender_id, receiver_id, amount);
