-- Create daily_summary table with all required fields
CREATE TABLE daily_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    
    -- Transfer statistics - initiated (all statuses)
    total_transfers_initiated BIGINT NOT NULL DEFAULT 0,
    total_amount_initiated DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    
    -- Transfer statistics - settled (successful only)
    total_transfers_settled BIGINT NOT NULL DEFAULT 0,
    total_amount_settled DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    
    -- Additional statistics
    fraud_alerts_raised BIGINT NOT NULL DEFAULT 0,
    new_user_registrations BIGINT NOT NULL DEFAULT 0,
    pending_settlement_count BIGINT NOT NULL DEFAULT 0,
    pending_settlement_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    
    -- Reconciliation data
    total_debits DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_credits DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    debit_count BIGINT NOT NULL DEFAULT 0,
    credit_count BIGINT NOT NULL DEFAULT 0,
    reconciliation_balanced BOOLEAN NOT NULL DEFAULT TRUE,
    reconciliation_mismatch DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_daily_summary_updated_at
BEFORE UPDATE ON daily_summary
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
