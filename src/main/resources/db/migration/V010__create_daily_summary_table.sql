-- Create daily_summary table
CREATE TABLE daily_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    total_transfers BIGINT NOT NULL DEFAULT 0,
    total_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    successful_transfers BIGINT NOT NULL DEFAULT 0,
    failed_transfers BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_daily_summary_updated_at
BEFORE UPDATE ON daily_summary
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
