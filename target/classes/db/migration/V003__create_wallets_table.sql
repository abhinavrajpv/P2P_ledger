-- Create wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    today_spending DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    daily_limit_reset_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on daily_limit_reset_date for daily limit reset job
CREATE INDEX idx_wallets_daily_limit_reset_date ON wallets(daily_limit_reset_date);

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_wallets_updated_at
BEFORE UPDATE ON wallets
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
