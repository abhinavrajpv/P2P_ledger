# Requirements Document

## Introduction

This is a simulated P2P payment ledger application where all wallet balances, transfers, and settlements are internal database records. No real money, bank accounts, UPI, payment gateways, or real financial settlement are involved. The system supports user registration, authentication, wallet management, peer-to-peer transfers, fraud detection, transaction history with optional masking, notifications, and administrative monitoring.

## Glossary

- **System**: P2P Payment Ledger Application
- **User**: Registered individual with name, email, phone, password, and 4-digit transaction PIN
- **Wallet**: Virtual balance account for each user, initialized to zero
- **Transaction**: Record of funds movement between users
- **Ledger Entry**: Detailed record of debits and credits for transactions
- **Transfer**: P2P fund movement from sender to receiver
- **Pending Transfer**: Transfer initiated but not yet credited to receiver
- **Settled Transfer**: Transfer completed and receiver credited
- **Admin**: User with elevated privileges for monitoring and management
- **Fraud Flag**: Transaction flagged for suspicious activity
- **Masking**: Feature to hide counterparty identifying details from transaction history
- **Scheduled Job**: Automated background task for settlement, limit reset, and reconciliation

## Requirements

### Requirement 1: Registration and Authentication

**User Story:** As a new user, I want to register with my personal details and transaction PIN, so that I can securely access the system.

#### Acceptance Criteria

1. WHEN a user submits registration with name, email, phone, password, and 4-digit transaction PIN, THEN the System SHALL create an account with automatic zero-balance wallet initialization
2. WHEN registration is submitted, THEN the System SHALL validate email format, phone format, password complexity, and PIN length
3. WHILE a user's account is ACTIVE, THEN the System SHALL allow login using email/phone and password
4. WHEN login succeeds, THEN the System SHALL generate and return a JWT access token
5. WHEN login fails with invalid credentials, THEN the System SHALL increment the failed login counter
6. IF the failed login counter reaches 5 consecutive failures, THEN the System SHALL lock the account for 30 minutes and reset the counter
7. WHEN account lockout period expires, THEN the System SHALL reset the failed login counter to zero
8. WHEN login succeeds, THEN the System SHALL reset the failed login counter to zero
9. WHEN a user is created via public registration, THEN the System SHALL assign USER role only
10. WHEN an admin promotes an existing USER to ADMIN, THEN the System SHALL update the role to ADMIN
11. WHEN an initial ADMIN account is provisioned, THEN the System SHALL create it using a documented development setup or database seed mechanism
12. WHEN public registration is submitted, THEN the System SHALL reject any role value in the request and always create a USER account
13. WHEN the React registration form is rendered, THEN the System SHALL NOT offer role selection - only USER is available
14. WHEN password is stored, THEN the System SHALL use secure hashing
15. WHEN transaction PIN is stored, THEN the System SHALL use secure hashing


### Requirement 2: Wallet Management

**User Story:** As a user, I want to manage my wallet with deposits and balance tracking, so that I can monitor my funds.

#### Acceptance Criteria

1. WHEN a user's wallet is created, THEN the System SHALL initialize balance to zero
2. WHEN a deposit is processed, THEN the System SHALL increase the wallet balance by the deposit amount
4. WHILE a wallet status is ACTIVE, THEN the System SHALL allow transactions
5. WHILE a wallet status is INACTIVE, THEN the System SHALL prevent all transactions
6. WHILE a wallet status is FROZEN, THEN the System SHALL prevent all transactions except unfreeze by admin
7. WHEN an admin unfreezes a wallet, THEN the System SHALL change status to ACTIVE
8. WHEN an admin freezes a wallet, THEN the System SHALL change status to FROZEN
9. WHEN a user deactivates their wallet, THEN the System SHALL change status to INACTIVE
10. WHEN a user reactivates their wallet, THEN the System SHALL change status to ACTIVE
11. WHEN wallet operations are performed, THEN the System SHALL maintain ledger entries for audit trail
12. WHEN a refund is processed, THEN the System SHALL follow PDF refund rules

### Requirement 3: Transfers

**User Story:** As a user, I want to send money to other users, so that I can perform P2P transactions.

#### Acceptance Criteria

1. WHEN a user searches for another user, THEN the System SHALL search by email or phone
2. WHEN a transfer is initiated, THEN the System SHALL verify the sender's 4-digit transaction PIN
3. WHEN PIN verification fails, THEN the System SHALL reject the transfer request
4. WHEN PIN verification succeeds, THEN the System SHALL debit the sender's wallet immediately
5. WHEN succeeds, THEN the System SHALL create a DEBIT ledger entry with PENDING status for the sender
6. WHEN  sender's debit succeeds, THE System SHALL atomically increase the sender wallet's todaySpending by the transfer amount.   
7. WHEN a transfer settlement succeeds, THEN the System SHALL credit the receiver's wallet and create a CREDIT ledger entry atomically
8. WHEN a transfer settlement fails or receiver wallet is ineligible (INACTIVE or FROZEN), THEN the System SHALL follow PDF decline/refund rules without creating a CREDIT entry
9. WHEN a transfer settlement is processed, THEN the System SHALL update transfer status to SUCCESS
10. WHEN a transfer settlement fails, THEN the System SHALL mark transfer as FAILED
11. WHEN a transfer is manually declined, THEN the System SHALL mark status as DECLINED
12. WHEN a transfer status is SUCCESS, THEN the System SHALL include amount in receiver's available balance
13. WHEN a transfer status is PENDING, FAILED, or DECLINED, THEN the System SHALL not include amount in receiver's available balance
14. WHEN the same authenticated user's request with the same requestId is received, THEN the System SHALL return the original transfer result without processing again (idempotency)
15. WHEN an authenticated sender submits a new requestId for the same recipient and amount within two minutes of an earlier transfer request, THE System SHALL flag and block the new request as a suspected duplicate.
16. WHEN a transfer request is submitted, THE System SHALL require a requestId and enforce its uniqueness per authenticated sender.
17. WHEN a transfer is initiated, THEN the System SHALL set settlement to occur in three business days
18. WHEN a transfer would cause sender's cumulative daily sending to exceed ₹50,000, THEN the System SHALL reject the transfer
19. WHEN a transfer amount exceeds ₹8,000, THEN the System SHALL flag the transaction for admin review (separate from daily limit check)
20. WHEN a transfer is flagged for fraud, THEN the System SHALL notify admin for review


### Requirement 4: Fraud Detection

**User Story:** As a user, I want the system to detect and flag suspicious transactions, so that potential fraud can be reviewed.

#### Acceptance Criteria

1. WHEN a transfer would cause sender's cumulative daily sending to exceed ₹50,000, THEN the System SHALL reject the transfer
2. WHEN a transfer amount exceeds ₹8,000, THEN the System SHALL flag the transaction for admin review (separate from daily limit)
3. WHEN the same sender submits a new requestId for the same recipient and amount within two minutes, THE System SHALL flag and block the request as a suspected duplicate.
4. WHEN a transfer is blocked due to fraud, THEN the System SHALL change status to DECLINED
5. WHEN an admin reviews a flagged transaction, THEN the System SHALL allow admin to approve or refund
6. WHEN a refund is processed for a flagged transaction, THEN the System SHALL follow PDF refund rules


### Requirement 5: Transaction History

**User Story:** As a user, I want to view my transaction history with filtering and details, so that I can track my financial activity.

#### Acceptance Criteria

1. WHEN a user requests transaction history, THEN the System SHALL return the last 10 transactions
2. WHEN pagination is requested, THEN the System SHALL support page and size parameters
3. WHEN date filters are provided, THEN the System SHALL filter transactions by date range
4. WHEN type filter is provided, THEN the System SHALL filter by transaction type
5. WHEN status filter is provided, THEN the System SHALL filter by transaction status
6. WHEN transaction details are requested, THEN the System SHALL return ledger entries for the transaction
7. WHEN days remaining is calculated, THEN the System SHALL show days until settlement for pending transfers
8. WHEN summary is requested, THEN the System SHALL return total sent amount, total received amount, and monthly transaction count
9. WHEN masking is enabled for user, THEN the System SHALL hide counterparty identifying details from transaction history, mini-statement, and transaction details
10. WHEN masking is enabled, THEN the System SHALL hide counterpartyId, counterpartyName, counterpartyEmail, counterpartyPhone, and other identifying information
11. WHEN masking is enabled, THEN the System SHALL keep amount, type, date, status, and settlement info visible
12. WHEN masking is enabled, THEN the System SHALL NOT send hidden identity details in API responses (mask at backend, not just CSS)
13. WHEN masking is enabled, THEN the System SHALL NOT include counterparty identifying details in nested ledger data
14. WHEN masking is disabled, THEN the System SHALL show complete counterparty details


### Requirement 6: NEW ENABLE MASKING FEATURE

**User Story:** As a user, I want to control whether my transaction history shows counterparty details, so that I can maintain privacy.

#### Acceptance Criteria

1. WHEN a user initially registers, THEN the System SHALL set masking preference to disabled
2. WHEN a user requests to enable masking, THEN the System SHALL verify the 4-digit transaction PIN
3. WHEN PIN verification for enable succeeds, THEN the System SHALL set masking preference to enabled
4. WHEN PIN verification for enable fails, THEN the System SHALL leave masking preference unchanged
5. WHEN a user requests to disable masking, THEN the System SHALL verify the 4-digit transaction PIN
6. WHEN PIN verification for disable succeeds, THEN the System SHALL set masking preference to disabled
7. WHEN PIN verification for disable fails, THEN the System SHALL leave masking preference unchanged
8. WHEN masking is enabled, THEN the System SHALL hide counterparty identifying details in transaction history, mini-statement, and transaction details for that user
9. WHEN masking is enabled, THEN the System SHALL keep amount, type, date, status, and settlement info visible
10. WHEN masking is enabled, THEN the System SHALL mask counterparty details in API responses before UI rendering
11. WHEN masking is enabled, THEN the System SHALL preserve ledger records unchanged
12. WHEN masking is enabled, THEN the System SHALL NOT modify another user's masking preference
13. WHEN masking is enabled, THEN the System SHALL preserve admin monitoring and reconciliation capabilities
14. WHEN masking is enabled, THEN the System SHALL NOT log or return PINs in any response
15. WHEN a user relogs after masking preference change, THEN the System SHALL persist the preference
16. WHEN admin queries data, THEN the System SHALL show complete counterparty details for reconciliation purposes

### Requirement 7: Notifications

**User Story:** As a user, I want to receive notifications for important events, so that I can stay informed about my account activity.

#### Acceptance Criteria

1. WHEN a transfer is initiated, THEN the System SHALL send notification to sender only (not receiver yet)
2. WHEN a transfer settlement is processed successfully, THEN the System SHALL send notification to both sender and receiver
3. WHEN a transfer fails, THEN the System SHALL send notification to sender
4. WHEN a transfer is declined, THEN the System SHALL send notification to sender
5. WHEN a refund is processed, THEN the System SHALL send notification to relevant parties
6. WHEN an account is locked due to failed logins, THEN the System SHALL send notification to user
7. WHEN an account is unlocked, THEN the System SHALL send notification to user
8. WHEN a notification is created, THEN the System SHALL mark it as unread
9. WHEN user requests unread count, THEN the System SHALL return count of unread notifications
10. WHEN user marks notification as read, THEN the System SHALL update notification status


### Requirement 8: Scheduled Jobs

**User Story:** As a system operator, I want automated background jobs, so that settlement, limit resets, and reconciliation happen automatically.

#### Acceptance Criteria

1. WHEN the hourly settlement job runs, THE System SHALL process eligible PENDING transfers whose settlement date has arrived, and credit the receiver only after settlement succeeds and any required admin review is approved.
2. WHEN the daily limit reset job runs, THEN the System SHALL reset daily fraud limits for all users
3. WHEN the unlock job runs every five minutes, THEN the System SHALL check for locked accounts past lockout period and unlock them
4. WHEN the daily summary job runs at 11 PM, THEN the System SHALL generate daily reconciliation and summary reports
5. WHEN any job runs, THEN the System SHALL log execution details including batch counts and results
6. WHEN settlement batch is processed, THEN the System SHALL log batch ID, count of transactions, and timing

### Requirement 9: Admin Dashboard and Monitoring

**User Story:** As an admin, I want monitoring and management capabilities, so that I can oversee system activity and handle exceptions.

#### Acceptance Criteria

1. WHEN an admin accesses dashboard, THEN the System SHALL display 30-day volume summary
2. WHEN dashboard is accessed, THEN the System SHALL display users count and wallets count
3. WHEN admin requests transaction monitoring, THEN the System SHALL show all transactions with filters
4. WHEN admin accesses settlement monitor, THEN the System SHALL show pending settlements
5. WHEN admin initiates manual settlement, THEN the System SHALL process specified transfers
6. WHEN admin accesses fraud review, THEN the System SHALL show flagged transactions
7. WHEN admin reviews flagged transaction, THEN the System SHALL allow approve or refund action
8. WHEN job monitor is accessed, THEN the System SHALL show scheduled job execution history
9. WHEN reconciliation is accessed, THEN the System SHALL show daily reconciliation reports
10. WHEN audit logs are accessed, THEN the System SHALL show system events and changes

## Technical Requirements

### Security

1. WHEN passwords are stored, THEN the System SHALL use secure hashing (e.g., bcrypt, Argon2)
2. WHEN transaction PINs are stored, THEN the System SHALL use secure hashing
3. WHEN JWT tokens are generated, THEN the System SHALL include user ID and role claims
4. WHEN token expires, THEN the System SHALL reject requests with expired token
5. WHEN unauthorized access is attempted, THEN the System SHALL return 401/403 status
6. WHEN masking is enabled, THEN the System SHALL never log or return PINs
### Database

The implementation plan MUST cover all 11 required tables:

1. **users** - User accounts with role, maskingEnabled, failedLoginCount, lockedUntil
2. **login_attempts** - Track failed login attempts for lockout mechanism
3. **wallets** - User wallet balances, walletStatus, and todaySpending tracking
4. **transactions** - Transfer records with requestId, status, settlementDate, fraudFlagged
5. **ledger_entries** - DEBIT and CREDIT entries for all balance changes
6. **fraud_alerts** - Flagged transactions for admin review
7. **notifications** - User notifications with read status
8. **audit_log** - System events and changes for audit trail
9. **scheduled_job_log** - Job execution history with batch IDs
10. **daily_summary** - Daily reconciliation and summary reports
11. **settlement_batches** - Settlement batch tracking

When wallet balance changes, THEN the System SHALL use transactions for atomic updates.
When ledger entries are created, THEN the System SHALL maintain audit trail.
When fraud limits are tracked, THEN the System SHALL use daily resets.
When scheduled jobs run, THEN the System SHALL log execution history.

### Performance

1. WHEN transaction history is requested, THEN the System SHALL respond within 2 seconds for 10 transactions
2. WHEN user search is performed, THEN the System SHALL respond within 1 second

### Availability

1. WHEN a user registers, THEN the System SHALL ensure unique email and phone
2. WHEN a transfer is initiated, THEN the System SHALL use optimistic locking to prevent race conditions
