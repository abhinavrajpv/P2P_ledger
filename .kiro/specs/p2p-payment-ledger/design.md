# Technical Design: P2P Payment Ledger

## Overview

This is a simulated P2P payment ledger application built with Java, Spring Boot 3.x, Spring Data JPA, Spring Security with JWT, and PostgreSQL. The frontend uses React with Vite. The application supports user registration, authentication, wallet management, simulated deposits, peer-to-peer transfers, fraud detection and review, transaction history with optional masking, notifications, scheduled settlement, reconciliation, and administrative monitoring. It does not support withdrawals or real-money movement.

## Architecture

### Layered Architecture

The application follows a clean layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│  - REST endpoints for all operations                        │
│  - Request validation and DTO mapping                       │
│  - Security annotations                                     │
├─────────────────────────────────────────────────────────────┤
│                    Service Layer                            │
│  - Business logic implementation                            │
│  - Transaction management                                   │
│  - Security checks and authorization                        │
├─────────────────────────────────────────────────────────────┤
│                    Repository Layer                         │
│  - JPA repositories for data access                         │
│  - Custom query methods                                     │
├─────────────────────────────────────────────────────────────┤
│                    Entity Layer                             │
│  - JPA entities for database tables                         │
│  - Relationships and constraints                            │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Controllers

| Component | Purpose |
|-----------|---------|
| AuthController | User registration and authentication |
| WalletController | Wallet operations and balance management |
| TransferController | P2P transfer creation and history |
| NotificationController | Notification management |
| AdminController | Admin dashboard and management operations |
| MaskingController | Masking preference management |

### Services

| Component | Purpose |
|-----------|---------|
| AuthService | User registration, login, JWT generation, and account lockout |
| WalletService | 	Wallet balance, simulated deposits, wallet status, and daily spending |
| TransferService | Transfer creation, validation,idempotency,settlement |
| FraudService | 	Duplicate detection, threshold checks, fraud alerts, and review workflow |
| NotificationService | Notification creation and management |
| MaskingService |	PIN-verified masking preference and user-specific response masking |
| ScheduledJobService | 	Settlement, daily-limit reset, account unlock, and daily-summary jobs |
| AuditService	| Record security-sensitive and administrative actions |
| ReconciliationService |	Reconcile transfers, ledger entries, and settlement batches|

### Repositories

| Component | Purpose |
|   ---     |   ---   |
| UserRepository | User data access |
| LoginAttemptsRepository | Login-attempt records and lockout tracking |
| WalletRepository | Wallet data access and locking/version-aware updates |
| TransferRepository | Transfer data access, idempotency, duplicate detection, and settlement queries |
| LedgerEntryRepository | Ledger-entry data access |
| FraudAlertRepository | Fraud alerts and review status |
| NotificationRepository | Notification data access |
| AuditLogRepository | Audit-log data access |
| ScheduledJobLogRepository | Job execution history |
| DailySummaryRepository | Daily aggregate summary data |
| SettlementBatchRepository | Settlement batch data access |

## Data Models

### User Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| name | String | Not null |
| email | String | Unique, not null |
| phone | String | Unique, not null |
| password | String | Not null; bcrypt-hashed |
| transactionPin | String | Not null; bcrypt-hashed 4-digit PIN |
| role | Enum | USER or ADMIN; public registration always assigns USER |
| maskingEnabled | Boolean | Default false |
| failedLoginCount | Integer | Default 0 |
| lockedUntil | LocalDateTime | Null when account is not locked |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |

### Wallet Entity

| Field | Type | Constraints |
    --- | ---  |    ---     |
| id | Long | Primary key |
| userId | Long | Unique foreign key to User |
| balance | BigDecimal | Not null; default 0.00 |
| todaySpending | BigDecimal | Not null; default 0.00 |
| dailyLimitResetDate | LocalDate | Date of the last daily-spending reset |
| status | Enum | ACTIVE, INACTIVE, or FROZEN |
| version | Long | JPA optimistic-locking version (`@Version`) |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |

### Transfer Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| senderId | Long | Foreign key to User |
| receiverId | Long | Foreign key to User |
| amount | BigDecimal | Not null; must be positive |
| requestId | String | Not null; unique within the authenticated sender's scope |
| status | Enum | PENDING, SUCCESS, FAILED, or DECLINED |
| createdAt | LocalDateTime | Creation timestamp |
| settlementDate | LocalDateTime | Three business days after creation |
| fraudFlagged | Boolean | Default false |
| adminReviewStatus | Enum | NOT_REQUIRED, PENDING, APPROVED, or REFUNDED |

### LedgerEntry Entity

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| transferId | Long | Nullable foreign key to Transfer; null for deposits  |
| userId | Long | FK to User |
| type | Enum | DEBIT, CREDIT |
| amount | BigDecimal | Not null |
| balanceBefore | BigDecimal | Not null |
| balanceAfter | BigDecimal | Not null |
| referenceId | String | Optional reference for deposits or other wallet operations |
| createdAt | LocalDateTime | |

### Notification Entity

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| userId | Long | FK to User |
| type | Enum | TRANSFER_INITIATED, SETTLEMENT, FAILED, REFUND, BLOCKED, LOCKED, UNLOCKED |
| message | String | Not null |
| isRead | Boolean | Default: false |
| createdAt | LocalDateTime | |

### ScheduledJobLog Entity

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| jobName | String | Not null |
| executionTime | LocalDateTime | Not null |
| status | Enum | SUCCESS, FAILED |
| details | String | JSON format, Not null |
| batchId | String | |

### LoginAttempts Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| userId | Long | Foreign key to User when the account is known; nullable for unknown identifiers if supported |
| identifier | String | Submitted login identifier |
| attemptedAt | LocalDateTime | Attempt timestamp |
| successful | Boolean | Whether authentication succeeded |
| source | String | Optional request source/IP metadata, subject to privacy-safe logging |

Use this entity to persist login-attempt history and support account lockout/security monitoring. Never store passwords or transaction PINs in it.

### FraudAlert Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| transferId | Long | Foreign key to Transfer |
| reason | String | Not null |
| status | Enum | OPEN, APPROVED, or REFUNDED/RESOLVED as defined in implementation |
| createdAt | LocalDateTime | Creation timestamp |
| reviewedBy | Long | Nullable foreign key to admin User until reviewed |
| reviewedAt | LocalDateTime | Null until reviewed |

### AuditLog Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| actorUserId | Long | Nullable foreign key to User for system actions |
| action | String | Not null |
| entityType | String | Type of affected entity |
| entityId | String | Identifier of affected record |
| details | String | Structured details; must not contain passwords, PINs, JWTs, or secrets |
| createdAt | LocalDateTime | Creation timestamp |

### DailySummary Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| summaryDate | LocalDate | Not null |
| totalTransfers | Long | Not null |
| totalAmount | BigDecimal | Not null |
| successfulTransfers | Long | Not null |
| failedTransfers | Long | Not null |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |

Define a unique constraint on summaryDate if one aggregate row per day is intended.

### SettlementBatch Entity

| Field | Type | Constraints |
|---|---|---|
| id | Long | Primary key |
| batchId | String | Unique, not null |
| startedAt | LocalDateTime | Batch start timestamp |
| completedAt | LocalDateTime | Null until complete |
| status | Enum | RUNNING, SUCCESS, or FAILED |
| processedCount | Integer | Number of transfers processed |
| failureCount | Integer | Number of processing failures |
| details | String | Summary of results; must not contain secrets |


## API Endpoints

### Authentication

#### POST /api/auth/register

Register a new user.

**Request Body:**
```java
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "password": "SecurePass123!",
  "transactionPin": "1234"
}
NOTE: Public registration always creates USER accounts. The backend ignores any client-supplied role and enforces role assignment server-side. Initial ADMIN accounts must be provisioned via documented development setup or database seed.

```

**Response:**
```java
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00"
}
```

#### POST /api/auth/login

Authenticate user and return JWT token.

**Request Body:**
```java
{
  "identifier": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```java
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "John Doe",
    "role": "USER"
  }
}
```

### Wallet

#### GET /api/wallet/balance

Get current wallet balance and today's spending.

**Response:**
```java
{
  "balance": 5000.00,
  "todaySpending": 2500.00,
  "dailyLimit": 50000.00,
  "status": "ACTIVE"
}
```

#### POST /api/wallet/deposit

Process a deposit (admin only for simulation).

**Request Body:**
```java
{
  "amount": 1000.00,
  "reference": "Initial deposit"
}
```

**Response:**
```java
{
  "id": 1,
  "amount": 1000.00,
  "newBalance": 6000.00,
  "reference": "Initial deposit"
}
```

#### PATCH /api/wallet/status

Users may deactivate an ACTIVE wallet or reactivate an INACTIVE wallet. Users cannot change a FROZEN wallet's status. Only an authorized ADMIN may freeze a wallet or unfreeze it to ACTIVE.

**Request Body:**
```java
{
  "status": "INACTIVE"
}
```

### User Management

#### GET /api/users/search

Search users by email or phone.

**Query Params:**
```
?query=john
```

**Response:**
```java
[
  {
    "id": 2,
    "name": "John Smith",
    "email": "john.smith@example.com",
    "phone": "9876543211"
  }
]
```

### Transfers

#### POST /api/transfers

Initiate a transfer. The client supplies a new requestId for each new transfer attempt. Reusing the same requestId for the same authenticated sender returns the original result and must not process the transfer again.

**Request Body:**
```java
{
  "receiverId": 2,
  "amount": 500.00,
  "pin": "1234",
  "requestId": "req_abc123"
}
```

**Response:**
```java
{
  "id": 1,
  "senderId": 1,
  "receiverId": 2,
  "amount": 500.00,
  "requestId": "req_abc123",
  "status": "PENDING",
  "settlementDate": "2024-01-18T00:00:00",
  "fraudFlagged": false
}
```
Apply the same masking rules to this response as to other user-facing transfer responses: when the authenticated sender has masking enabled, do not return counterparty identifiers. The server derives the sender identity from the authenticated principal and must not accept a client-supplied senderId.

#### GET /api/transfers/{id}

Get transfer details including ledger entries, subject to authorization and the requesting user's masking preference.

The response must be scoped to the authenticated user unless the caller is an administrator. For a USER, only the sender or receiver of the transfer may access it. If masking is enabled for that user, omit counterparty identifiers such as counterparty ID, name, email, and phone from the response, including nested ledger-related DTOs. Continue returning amount, transaction type, status, timestamps, settlement information, and ledger amounts/balances where authorized. Never return PINs, password hashes, or secrets.

Example response when masking is disabled (simplified):

**Response:**
```java
{
  "id": 1,
  "senderId": 1,
  "receiverId": 2,
  "amount": 500.00,
  "status": "SUCCESS",
  "settlementDate": "2024-01-18T00:00:00",
  "ledgerEntries": [
    {
      "type": "DEBIT",
      "amount": 500.00,
      "balanceBefore": 5500.00,
      "balanceAfter": 5000.00
    },
    {
      "type": "CREDIT",
      "amount": 500.00,
      "balanceBefore": 0.00,
      "balanceAfter": 500.00
    }
  ]
}
```
When masking is enabled, omit counterparty identifiers from the USER-facing response rather than returning null or placeholder identifiers. ADMIN responses may contain full details.

#### GET /api/transfers

Get transaction history with filters.

**Query Params:**
```
?page=0&size=10&type=DEBIT&status=SUCCESS&startDate=2024-01-01&endDate=2024-01-31
```

**Response:**
```java
{
  "content": [
    {
      "id": 1,
      "amount": 500.00,
      "type": "DEBIT",
      "status": "SUCCESS",
      "createdAt": "2024-01-15T10:30:00",
      "settlementDate": "2024-01-18T00:00:00",
      "daysRemaining": 3,
      "masked": false
    }
  ],

  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

#### GET /api/transfers/summary

Get transaction summary.

**Response:**
```java
{
  "totalSent": 5000.00,
  "totalReceived": 3500.00,
  "monthlyCount": 15
}
```

### Masking

#### GET /api/masking/status

Get current masking preference.

**Response:**
```java
{
  "enabled": false
}
```

#### POST /api/masking/enable

Enable masking with PIN verification.

**Request Body:**
```java
{
  "pin": "1234"
}
```

**Response:**
```java
{
  "enabled": true
}
```

#### POST /api/masking/disable

Disable masking with PIN verification.

**Request Body:**
```java
{
  "pin": "1234"
}
```

**Response:**
```java
{
  "enabled": false
}
```

### Notifications

#### GET /api/notifications/unread-count

Get count of unread notifications.

**Response:**
```java
{
  "unreadCount": 5
}
```

#### GET /api/notifications

Get notification history.

**Query Params:**
```
?page=0&size=10
```

**Response:**
```java
{
  "content": [
    {
      "id": 1,
      "type": "TRANSFER_INITIATED",
      "message": "Transfer of ₹500 initiated to John Smith",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

#### PATCH /api/notifications/{id}/read

Mark notification as read.

### Admin Endpoints

#### GET /api/admin/dashboard

Get admin dashboard data.

**Response:**
```java
{
  "volume30Days": 250000.00,
  "usersCount": 150,
  "walletsCount": 150,
  "pendingSettlements": 25,
  "flaggedTransactions": 12
}
```

#### GET /api/admin/transactions

Get all transactions (admin view).

#### GET /api/admin/settlements

Get pending settlements.

#### POST /api/admin/settlements/process

Process settlements manually.

#### GET /api/admin/fraud

Get flagged transactions for review.

#### POST /api/admin/fraud/{id}/approve

Approve flagged transaction.

#### POST /api/admin/fraud/{id}/refund

Refund flagged transaction.

#### GET /api/admin/jobs

Get scheduled job execution history.

#### GET /api/admin/reconciliation

Get reconciliation reports.

## Security Implementation

### JWT Authentication

- Tokens issued on login with 1-hour expiry
- Roles: USER, ADMIN
- Token validation on all protected endpoints
- Refresh tokens not implemented for simplicity

### Password and PIN Security

- Passwords hashed with bcrypt (strength 12)
- Transaction PINs hashed with bcrypt
- PIN never returned in responses
- PIN never logged

### Authorization

- USER role: Access own resources only
- ADMIN role: Access all resources including admin endpoints
- PIN verification required for sensitive operations (transfers, masking toggle)

## Fraud Detection Logic

### Daily Limits

- Cumulative daily sending limit: ₹50,000 per user. Reject a transfer if it would cause the user's total daily sending to exceed ₹50,000.
- Admin review threshold: Transfers exceeding ₹8,000 are flagged for admin review. This is separate from the cumulative daily limit.
- Daily reset: Reset daily spending tracking at the configured daily reset time. Do not treat either threshold as a per-transfer daily-reset value.

### Duplicate Detection

- Idempotency: A requestId must be unique within the authenticated sender's scope. Repeating the same requestId with the same request details returns the original result without processing the transfer again. Reusing it with different request details must be rejected as a conflict.
- Duplicate detection: A new requestId for the same recipient and amount within two minutes is treated as a suspected duplicate and flagged or blocked according to the project requirements.
- Idempotency and duplicate detection are separate checks.

### Fraud Review Flow

1. When a transfer exceeds the configured admin-review threshold, flag it for review during creation.
2. Debit the sender and create the corresponding DEBIT ledger entry. Do not credit the receiver while the transfer awaits review.
3. Set `fraudFlagged = true` and `adminReviewStatus = PENDING`.
4. The administrator reviews the flagged transfer and chooses to approve or reject it.
5. **If approved:** Set `adminReviewStatus = APPROVED`. Keep the transfer `PENDING` until its scheduled settlement date. The settlement job may then process it, provided all settlement conditions are satisfied.
6. **If rejected before settlement:** Refund the sender immediately, create a corresponding refund ledger entry, set the transfer status to `DECLINED`, and set `adminReviewStatus = REFUNDED`. Do not credit or debit the receiver.
7. Perform the refund, wallet balance update, ledger entry creation, and status updates atomically. Ensure retries cannot refund the same transfer more than once.
8. Record the administrator's decision in the fraud alert and audit log, and notify the relevant user.
9. The settlement job must skip declined, refunded, or otherwise ineligible transfers.

## Masking Implementation

### Data Model

- Added `maskingEnabled` boolean field to User entity
- Default value: false
- User can toggle with PIN verification

### Backend Masking

- The backend checks the authenticated user's own maskingEnabled preference when building transaction-history, mini-statement, and transfer-detail responses.
- When masking is enabled, omit all counterparty identifiers (ID, name, email, phone, and equivalent nested fields) from that user's API response. Do not send the hidden values to the frontend.
- Continue returning transaction amount, type, date/time, status, and settlement information.
- Do not alter or delete underlying User, Transfer, or LedgerEntry records when masking is toggled.
- Masking applies only to the requesting user's view; it must not change another user's preference or response.
- ADMIN monitoring and reconciliation endpoints return full details to authorized administrators.
- Never log or return transaction PINs, password hashes, JWTs, or other secrets.

### API Response Masking

```java
When masking enabled:
```java
{
  "amount": 500.00,
  "type": "DEBIT",
  "status": "SUCCESS",
  ...
}
```
NOTE: All counterparty identifiers (id, name, email, phone) are omitted when masking is enabled. Admin queries always return full details.

## Scheduled Jobs

### Job Configuration

- **Settlement Job**: Hourly, processes eligible pending transfers
- **Daily Limit Reset**: Daily at midnight
- **Account Unlock**: Every 5 minutes
- **Daily Summary**: Daily at 11 PM

### Job Execution

- Each job logs execution to ScheduledJobLog table
- Batch processing for efficiency
- Error handling with retry logic

## Error Handling

### Standard Error Response

```java
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid transaction PIN",
  "path": "/api/transfers"
}
```

### HTTP Status Codes

- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 409: Conflict (duplicate request ID)
- 500: Internal Server Error

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Transfer Amount Consistency

For any transfer, the sender debit and its DEBIT ledger entry are created atomically at initiation. While the transfer is PENDING, the receiver balance is unchanged and no receiver CREDIT ledger entry exists. When settlement succeeds, the receiver balance increases by the transfer amount and a matching CREDIT ledger entry is created atomically. Failed, declined, or review-pending transfers must not create a receiver CREDIT entry.

**Validates: Requirements 3.4–3.13 and 8.1**

### Property 2: Ledger Entry Balance Tracking

For any ledger entry, balanceAfter equals balanceBefore - amount for a DEBIT and balanceBefore + amount for a CREDIT. Wallet balance changes and corresponding ledger entries must be committed atomically.

**Validates: Requirements 2.2, 2.11, 3.5–3.8**



### Property 3: Daily Spending Reset

For any wallet, at the start of each new day (after daily limit reset job), the todaySpending should be reset to zero while the balance remains unchanged.

**Validates: Requirements 8.2**

### Property 4: Masking Does Not Alter Data

For any user with masking enabled, when admin queries the same data, the complete counterparty details should be returned, and the underlying ledger records should remain unchanged.

**Validates: Requirements 6.11, 6.12, 6.13**

### Property 5: Pin Verification Preserves Preference

For any masking toggle request with an invalid PIN, the user's masking preference should remain unchanged after the request completes.


### Property 6: Transfer Idempotency

For any authenticated user's transfer request with a given requestId, repeating the same requestId must return the original transfer result without processing the transfer again, regardless of the time elapsed.

**Validates: Requirement 3.14**

### Property 7: Duplicate Transfer Detection

For any authenticated user, a new requestId for the same amount and recipient within two minutes must be flagged and blocked as a suspected duplicate. This is separate from idempotency.

**Validates: Requirement 3.15**

## Testing Strategy

### Unit Tests

- Service layer business logic
- Repository query methods
- Security authorization
- Masking logic

### Integration Tests

- API endpoints with full context
- Database operations
- Transaction management
- Scheduled jobs

### Property-Based Tests

- Transfer round-trip consistency
- Ledger entry balance verification
- Daily limit calculations
