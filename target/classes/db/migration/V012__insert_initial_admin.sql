-- Insert initial admin user (runs once, idempotent)
-- Admin credentials:
--   Email: admin@p2pledger.com
--   Password: Admin@123
--   PIN: 1234

-- Only insert if admin doesn't exist (idempotent)
INSERT INTO users (name, email, phone, password, transaction_pin, role, masking_enabled, failed_login_count, created_at, updated_at)
SELECT 
    'System Admin',
    'admin@p2pledger.com',
    '9999999999',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYPNd6U9.aW', -- BCrypt hash of "Admin@123"
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYPNd6U9.aW', -- BCrypt hash of "1234"
    'ADMIN',
    false,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@p2pledger.com' OR phone = '9999999999' OR role = 'ADMIN'
);

-- Create wallet for admin user
INSERT INTO wallets (user_id, balance, today_spending, daily_limit_reset_date, status, version, created_at, updated_at)
SELECT 
    u.id,
    0.00,
    0.00,
    CURRENT_DATE,
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'admin@p2pledger.com'
AND NOT EXISTS (
    SELECT 1 FROM wallets WHERE user_id = u.id
);
