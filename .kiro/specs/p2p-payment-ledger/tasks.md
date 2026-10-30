# Implementation Plan: P2P Payment Ledger

## Overview

This document outlines the implementation tasks for the P2P Payment Ledger application using Java, Spring Boot 3.x, Spring Data JPA, Spring Security with JWT, PostgreSQL, and React with Vite. The project follows a layered architecture with Controllers, Services, Repositories, and Entities.

## Tasks

- [ ] 1. Set up project structure and dependencies
  - Create Maven project structure with Spring Boot 3.x
  - Add dependencies: Spring Data JPA, Spring Security, JWT, PostgreSQL, Lombok
  - Create Maven multi-module structure if needed (backend/api, backend/services, backend/models)
  - Create React/Vite project structure
  - _Requirements: Technical Requirements (Performance, Availability)_

- [ ] 2. Configure database and JPA
  - [ ] 2.1 Configure PostgreSQL connection in application.properties
    - Set datasource URL, username, password
    - Configure JPA dialect and ddl-auto
    - _Requirements: Technical Requirements (Database)_
  
 - [ ] 2.2 Create database migration scripts
    - Create tables for all 11 required entities: User, LoginAttempts, Wallet, Transfer, LedgerEntry, FraudAlert, Notification, AuditLog, ScheduledJobLog, DailySummary and SettlementBatch.
    - Add appropriate primary keys, foreign keys, unique constraints, indexes and status constraints.
    - Enforce requestId uniqueness within the authenticated sender's scope.
    - Add indexes needed for duplicate-transfer detection and settlement processing.
    - Ensure migration scripts match the entity definitions.
    - *Requirements: Requirement 1, Requirement 2, Requirement 3, Requirement 4, Requirement 7, Requirement 8, Requirement 9, Technical Requirements (Database)*

- [ ] 3. Implement core domain entities

  - [ ] 3.1 Create User entity
    - Fields: id, name, email, phone, password, transactionPin, role, maskingEnabled, failedLoginCount, lockedUntil.
    - Public registration always creates USER accounts; never trust a role supplied by the client.
    - *Requirements: Requirement 1, Requirement 6*

  - [ ] 3.2 Create Wallet entity
    - Fields: id, userId, balance, todaySpending, dailyLimitResetDate, status.
    - Support ACTIVE, INACTIVE and FROZEN states.
    - Include a JPA @Version field for optimistic locking to help prevent concurrent wallet updates from overwriting each other.
    - *Requirements: Requirement 2, Requirement 4, Requirement 8*

  - [ ] 3.3 Create Transfer entity
    - Fields: id, senderId, receiverId, amount, requestId, status, createdAt, settlementDate, fraudFlagged, adminReviewStatus.
    - Enforce requestId uniqueness per authenticated sender.
    - Add indexes for duplicate detection and settlement processing.
    - *Requirements: Requirement 3, Requirement 4*

  - [ ] 3.4 Create LedgerEntry entity
    - Fields: id, transferId, userId, type, amount, balanceBefore, balanceAfter, referenceId, status.
    - Support DEBIT and CREDIT entries with appropriate constraints.
    - *Requirements: Requirement 2, Requirement 3*

  - [ ] 3.5 Create Notification entity
    - Fields: id, userId, type, message, isRead, createdAt.
    - *Requirements: Requirement 7*

  - [ ] 3.6 Create ScheduledJobLog entity
    - Store job name, execution time, status, batch counts, result and error details where applicable.
    - *Requirements: Requirement 8, Requirement 9*

  - [ ] 3.7 Create LoginAttempts entity
    - Track failed login attempts and account-locking events.
    - *Requirements: Requirement 1*

  - [ ] 3.8 Create FraudAlert entity
    - Track the triggering rule, related transfer, review status and resolution.
    - *Requirements: Requirement 4, Requirement 9*

  - [ ] 3.9 Create AuditLog entity
    - Record relevant system and administrative actions.
    - *Requirements: Requirement 9*

  - [ ] 3.10 Create DailySummary entity
    - Store daily summary and reconciliation results.
    - *Requirements: Requirement 8, Requirement 9*

  - [ ] 3.11 Create SettlementBatch entity
    - Store batch identifier, processing status, counts, timing and results.
    - *Requirements: Requirement 8, Requirement 9*
- [ ] 4. Implement JPA repositories
  - [ ] 4.1 Create UserRepository with custom queries
    - Add methods forfindByEmail, findByPhone, findByFailedLoginCountGreaterThan
    - _Requirements: Requirement 1_
  
  - [ ] 4.2 Create WalletRepository with custom queries
    - Add methods for findByUserId, findByDailyLimitResetDateBefore
    - _Requirements: Requirement 2, Requirement 8_
  
  - [ ] 4.3 Create TransferRepository with custom queries
    - Add methods for findBySenderId, findByReceiverId, findByRequestId, findByStatus
    - _Requirements: Requirement 3, Requirement 4_
  
  - [ ] 4.4 Create LedgerEntryRepository with custom queries
    - Add methods for findByTransferId, findByUserId
    - _Requirements: Requirement 2, Requirement 5_
  
  - [ ] 4.5 Create NotificationRepository with custom queries
    - Add methods for findByUserIdOrderByCreatedAtDesc, findByIsReadFalseCountByUserId
    - _Requirements: Requirement 7_
  
  - [ ] 4.6 Create ScheduledJobLogRepository
    - Add methods for findByJobNameOrderByExecutionTimeDesc
    - _Requirements: Requirement 8_

    - [ ] 4.7 Create LoginAttemptsRepository
  - Add methods needed to track failed login attempts and account-locking events.
  - Requirements: Requirement 1

- [ ] 4.8 Create FraudAlertRepository
  - Add methods to retrieve alerts by status and related transfer.
  - Requirements: Requirement 4, Requirement 9

- [ ] 4.9 Create AuditLogRepository
  - Add methods to retrieve audit records using relevant filters.
  - Requirements: Requirement 9

- [ ] 4.10 Create DailySummaryRepository
  - Add methods to retrieve daily summary and reconciliation records.
  - Requirements: Requirement 8, Requirement 9

- [ ] 4.11 Create SettlementBatchRepository
  - Add methods to retrieve settlement batches and their processing results.
  - Requirements: Requirement 8, Requirement 9

- [ ] 5. Implement authentication service
  - [ ] 5.1 Create password encoder with bcrypt
    - Configure BCryptPasswordEncoder with strength 12
    - _Requirements: Requirement 1.10, Requirement 1.11_
  
  - [ ] 5.2 Implement registration logic
    - Validate email, phone, password, PIN
    - Hash password and PIN
    - Create user with zero-balance wallet
    - _Requirements: Requirement 1.1, Requirement 1.2_
  
  - [ ] 5.3 Implement login logic with failed login tracking
    - Validate credentials
    - Increment failed login counter on failure
    - Lock account after 5 consecutive failures for 30 minutes
    - _Requirements: Requirement 1.5, Requirement 1.6, Requirement 1.7, Requirement 1.8_
  
  - [ ] 5.4 Implement JWT token generation
    - Create JWT utility class
    - Generate tokens with user ID and role claims
    - _Requirements: Requirement 1.4_

- [ ] 6. Implement wallet service
  - [ ] 6.1 Implement balance retrieval logic
    - Get wallet balance and today's spending
    - _Requirements: Requirement 2.1, Requirement 2.2_
  
  - [ ] 6.2 Implement deposit logic
    - Update wallet balance
    - Create ledger entry
    - _Requirements: Requirement 2.2_
  
  - [ ] 6.3 Implement wallet status management
    - Update wallet status (ACTIVE, INACTIVE, FROZEN)
    - _Requirements: Requirement 2.4, Requirement 2.5, Requirement 2.6, Requirement 2.7, Requirement 2.8, Requirement 2.9, Requirement 2.10_

- [ ] 7. Implement transfer service
 - [ ] 7.1 Implement PIN verification
    - Verify the transaction PIN before transfers.
    - Verify the transaction PIN before enabling or disabling masking.
    - Change the masking preference only after successful PIN verification.
    - Leave the existing preference unchanged when the PIN is invalid.
    - _Requirements: Requirement 3.2, Requirement 6.2, Requirement 6.5_
  
  - [ ] 7.2 Implement user search
    - Search users by email or phone
    - _Requirements: Requirement 3.1_

   - [ ] 7.3 Implement transfer creation with PIN
    - Verify transaction PIN.
    - Validate sender and receiver wallet statuses.
    - Check the cumulative daily sending limit of ₹50,000 per sender.
    - Reject transfers that would exceed the remaining daily limit.
    - Flag transfers exceeding ₹8,000 for admin review.
    - Debit the sender wallet atomically.
    - Create the sender's DEBIT ledger entry with PENDING status.
    - Do not create the receiver's CREDIT ledger entry at initiation.
    - Set settlement date to three business days after initiation.
    - Enforce idempotency and separate duplicate-transfer detection.
    - _Requirements: Requirement 3.3, Requirement 3.4, Requirement 3.5, Requirement 3.6, Requirement 3.14, Requirement 3.15, Requirement 3.17, Requirement 3.18, Requirement 3.19, Requirement 4_
  
  - [ ] 7.4 Implement fraud detection
    - Flag transfers exceeding ₹8,000 for admin review.
    - Reject transfers that would cause the sender's cumulative daily sending to exceed ₹50,000.
    - Detect suspected duplicates when a new requestId is used for the same sender, recipient and amount within two minutes.
    - Treat repeated use of the same requestId as idempotency, returning the original result without processing the transfer twice.
    - Record fraud alerts and their triggering rules.
    - Ensure flagged transfers follow the defined admin-review and settlement workflow.
    - *Requirements: Requirement 3, Requirement 4*

- [ ] 7.5 Implement settlement processing
    - Process only eligible PENDING transfers whose settlement date has arrived.
    - Hold flagged transfers until required admin approval.
    - Recheck receiver wallet status before settlement.
    - Atomically credit the receiver and create the receiver's CREDIT ledger entry only when settlement succeeds.
    - Do not expect a pending receiver CREDIT ledger entry to exist before settlement.
    - If the receiver wallet is INACTIVE or FROZEN at settlement, apply the defined decline/refund rules. If an admin rejects a flagged transfer before settlement, immediately refund the sender, create a CREDIT refund ledger entry, set the transfer status to DECLINED and admin review status to REFUNDED, and do not credit the receiver. Keep the original sender DEBIT entry. Perform the refund atomically and ensure retries cannot refund twice.
    - Make settlement and refunds idempotent so retries cannot apply credits or refunds twice.
    - Update transfer and ledger statuses consistently.
    - Log settlement results in the appropriate settlement batch and scheduled job records.
    - *Requirements: Requirement 2, Requirement 3, Requirement 4, Requirement 8, Requirement 9*  

    - [ ] 7.6 Implement transaction history
    - Retrieve transaction history with pagination and filters.
    - Calculate days remaining until settlement.
    - Calculate total sent, total received and monthly transaction count.
    - Apply the user's masking preference on the backend.
    - When masking is enabled, omit counterparty IDs, names, email addresses, phone numbers and other identifying information from API responses, including nested ledger data.
    - Preserve amounts, transaction types, dates, statuses and settlement information.
    - Ensure users can retrieve only their own transaction history.
    - _Requirements: Requirement 5.1, Requirement 5.2, Requirement 5.3, Requirement 5.4, Requirement 5.5, Requirement 5.6, Requirement 5.7, Requirement 5.8, Requirement 5.9, Requirement 5.10, Requirement 5.11, Requirement 5.12, Requirement 5.13, Requirement 5.14, Requirement 6.8, Requirement 6.9, Requirement 6.10, Requirement 6.11, Requirement 6.12, Requirement 6.13_

- [ ] 8. Implement masking service
  - [ ] 8.1 Implement masking status retrieval
    - Get user's maskingEnabled preference
    - _Requirements: Requirement 6.1_
  
  - [ ] 8.2 Implement masking enable with PIN verification
    - Verify PIN
    - Update maskingEnabled to true
    - _Requirements: Requirement 6.2, Requirement 6.3, Requirement 6.4_
  
  - [ ] 8.3 Implement masking disable with PIN verification
    - Verify PIN
    - Update maskingEnabled to false
    - _Requirements: Requirement 6.5, Requirement 6.6, Requirement 6.7_
  
  - [ ] 8.4 Implement transaction history masking
    - Check user's maskingEnabled
    - Mask counterparty details in API responses
    - Keep amount, type, date, status, settlement info
    - _Requirements: Requirement 5.9, Requirement 5.10, Requirement 5.11, Requirement 6.8, Requirement 6.9, Requirement 6.10_

- [ ] 9. Implement notification service
  - [ ] 9.1 Implement notification creation
    - Create notifications for transfers, settlements, failures, refunds, blocks, lock/unlock
    - _Requirements: Requirement 7.1, Requirement 7.2, Requirement 7.3, Requirement 7.4, Requirement 7.5, Requirement 7.6, Requirement 7.7_
  
  - [ ] 9.2 Implement unread count
    - Count unread notifications
    - _Requirements: Requirement 7.8, Requirement 7.9_
  
  - [ ] 9.3 Implement mark as read
    - Update notification isRead status
    - _Requirements: Requirement 7.10_

- [ ] 10. Implement admin service
  - [ ] 10.1 Implement dashboard data
    - Get 30-day volume
    - Get users count
    - Get wallets count
    - _Requirements: Requirement 9.1, Requirement 9.2_
  
  - [ ] 10.2 Implement transaction monitoring
    - Get all transactions with filters
    - _Requirements: Requirement 9.3_
  
  - [ ] 10.3 Implement settlement monitoring
    - Get pending settlements
    - Process settlements manually
    - _Requirements: Requirement 9.4, Requirement 9.5_
  
  - [ ] 10.4 Implement fraud review
    - Get flagged transactions
    - Approve or refund transactions
    - _Requirements: Requirement 9.6, Requirement 9.7, Requirement 4.5, Requirement 4.6_
  
  - [ ] 10.5 Implement job execution history
    - Get scheduled job logs
    - _Requirements: Requirement 9.8_
  
  - [ ] 10.6 Implement reconciliation
    - Generate reconciliation reports
    - _Requirements: Requirement 9.9_

- [ ] 11. Implement scheduled jobs
  - [ ] 11.1 Implement hourly settlement job
    - Process eligible pending transfers
    - Credit receiver wallets
    - Log execution
    - _Requirements: Requirement 8.1_
  - [ ] 11.2 Implement daily limit reset job
    - Reset todaySpending for all applicable wallets at the configured daily reset time.
    - Ensure the ₹50,000 cumulative daily sending limit is calculated correctly for each day.
    - Keep the ₹8,000 admin-review threshold separate from the daily sending limit.
    - Record the job execution in scheduled_job_log.
    - _Requirements: Requirement 8.2_
  
  - [ ] 11.3 Implement account unlock job
    - Check for locked accounts past lockout period
    - Unlock accounts
    - Reset failed login counter
    - _Requirements: Requirement 8.3_
  
  - [ ] 11.4 Implement daily summary job
    - Generate daily reconciliation and summary reports
    - Log execution
    - _Requirements: Requirement 8.4, Requirement 8.5, Requirement 8.6_

- [ ] 12. Implement REST controllers
  - [ ] 12.1 Implement AuthController
    - POST /api/auth/register
    - POST /api/auth/login
    - _Requirements: Requirement 1_
  
  - [ ] 12.2 Implement WalletController
    - GET /api/wallet/balance
    - POST /api/wallet/deposit
    - PATCH /api/wallet/status
    - _Requirements: Requirement 2_
  
  - [ ] 12.3 Implement TransferController
    - GET /api/users/search
    - POST /api/transfers
    - GET /api/transfers/{id}
    - GET /api/transfers
    - GET /api/transfers/summary
    - _Requirements: Requirement 3, Requirement 5_
  
  - [ ] 12.4 Implement MaskingController
    - GET /api/masking/status
    - POST /api/masking/enable
    - POST /api/masking/disable
    - _Requirements: Requirement 6_
  
  - [ ] 12.5 Implement NotificationController
    - GET /api/notifications/unread-count
    - GET /api/notifications
    - PATCH /api/notifications/{id}/read
    - _Requirements: Requirement 7_
  
  - [ ] 12.6 Implement AdminController
    - GET /api/admin/dashboard
    - GET /api/admin/transactions
    - GET /api/admin/settlements
    - POST /api/admin/settlements/process
    - GET /api/admin/fraud
    - POST /api/admin/fraud/{id}/approve
    - POST /api/admin/fraud/{id}/refund
    - GET /api/admin/jobs
    - GET /api/admin/reconciliation
    - _Requirements: Requirement 9_

- [ ] 13. Implement security configuration
  - [ ] 13.1 Configure JWT authentication filter
    - Extract token from Authorization header
    - Validate token and extract user
    - Set security context
    - _Requirements: Technical Requirements (Security)_
  
  - [ ] 13.2 Configure authorization rules
    - USER role: access own resources
    - ADMIN role: access admin endpoints
    - _Requirements: Technical Requirements (Security)_
  
  - [ ] 13.3 Configure PIN verification for sensitive operations
    - Add custom annotation and aspect for PIN verification
    - _Requirements: Technical Requirements (Security)_

- [ ] 14. Create React components (frontend)
  - [ ] 14.1 Create authentication components
    - Registration form
    - Login form
    - _Requirements: Requirement 1_
  
  - [ ] 14.2 Create wallet components
    - Balance display
    - Deposit form
    - Wallet status toggle
    - _Requirements: Requirement 2_
  
  - [ ] 14.3 Create transfer components
    - User search
    - Transfer form with PIN
    - Transaction history list
    - Summary display
    - _Requirements: Requirement 3, Requirement 5_
  
  - [ ] 14.4 Create masking toggle component
    - Enable/disable toggle with PIN verification
    - _Requirements: Requirement 6_
  
  - [ ] 14.5 Create notification components
    - Unread count badge
    - Notification list
    - Mark as read
    - _Requirements: Requirement 7_
  
  - [ ] 14.6 Create admin dashboard
    - Volume summary
    - Users/wallets count
    - Transaction monitoring
    - Settlement monitor
    - Fraud review
    - Job monitor
    - Reconciliation
    - _Requirements: Requirement 9_

- [ ] 15. Implement masking in React UI
- [ ] 15.1 Create masking-aware transaction list
    - Show or hide counterparty details according to the authenticated user's masking preference.
    - When masking is enabled, do not expose counterparty IDs, names, email addresses, phone numbers or other identifying details.
    - Keep amount, transaction type, date, status and settlement information visible.
    - Ensure the backend API responses are masked, not merely the frontend display.
    - Preserve full transaction details for authorized admin monitoring.
    - _Requirements: Requirement 5.9, Requirement 5.10, Requirement 5.11, Requirement 5.12, Requirement 5.13, Requirement 5.14, Requirement 6.8, Requirement 6.9, Requirement 6.10, Requirement 6.11, Requirement 6.12, Requirement 6.13_
  
  - [ ] 15.2 Implement masking toggle UI
    - Show current masking status
    - Toggle with PIN verification
    - _Requirements: Requirement 6.2, Requirement 6.5_

- [ ] 16. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Write property tests for correctness

  - [ ] 17.1 Write property test for transfer amount consistency
    - Verify sender debit and receiver credit occur at the correct lifecycle stages.
    - *Requirements: Requirement 3*

  - [ ] 17.2 Write property test for ledger entry balance tracking
    - Verify ledger balance-before and balance-after calculations.
    - *Requirements: Requirement 2, Requirement 3*

  - [ ] 17.3 Write property test for daily spending reset
    - Verify daily spending resets without changing wallet balance.
    - *Requirements: Requirement 8*

  - [ ] 17.4 Write property test for masking data preservation
    - Verify masking changes API output only, not stored ledger records.
    - *Requirements: Requirement 5, Requirement 6*

  - [ ] 17.5 Write property test for PIN verification preference preservation
    - Verify invalid PIN leaves masking preference unchanged.
    - *Requirements: Requirement 6*

  - [ ] 17.6 Write property test for transfer idempotency
    - Verify repeating the same requestId returns the original result and never processes the transfer twice.
    - *Requirements: Requirement 3*

  - [ ] 17.7 Write property test for duplicate transfer detection
    - Verify a new requestId for the same sender, recipient and amount within two minutes is flagged and blocked.
    - *Requirements: Requirement 3, Requirement 4*

  - [ ] 17.8 Write property test for settlement and refund idempotency
    - Verify repeated settlement or refund attempts cannot apply a credit or refund twice.
    - *Requirements: Requirement 2, Requirement 3, Requirement 8*

- [ ] 18. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["2.2", "3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", "3.9", "3.10", "3.11"] },
    { "id": 2, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9", "4.10", "4.11"] },
    { "id": 3, "tasks": ["5.1", "5.2", "5.3", "5.4"] },
    { "id": 4, "tasks": ["6.1", "6.2", "6.3"] },
    { "id": 5, "tasks": ["7.1", "7.2", "7.3", "7.4", "7.5", "7.6"] },
    { "id": 6, "tasks": ["8.1", "8.2", "8.3", "8.4"] },
    { "id": 7, "tasks": ["9.1", "9.2", "9.3"] },
    { "id": 8, "tasks": ["10.1", "10.2", "10.3", "10.4", "10.5", "10.6"] },
    { "id": 9, "tasks": ["11.1", "11.2", "11.3", "11.4"] },
    { "id": 10, "tasks": ["12.1", "12.2", "12.3", "12.4", "12.5", "12.6"] },
    { "id": 11, "tasks": ["13.1", "13.2", "13.3"] },
    { "id": 12, "tasks": ["14.1", "14.2", "14.3", "14.4", "14.5", "14.6"] },
    { "id": 13, "tasks": ["15.1", "15.2"] },
    { "id": 14, "tasks": ["16"] },
    { "id": 15, "tasks": ["17.1", "17.2", "17.3", "17.4", "17.5", "17.6", "17.7", "17.8"] },
    { "id": 16, "tasks": ["18"] }
  ]
}
```
