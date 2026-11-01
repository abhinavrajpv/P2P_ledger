package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScheduledJobLogRepository scheduledJobLogRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final SettlementBatchRepository settlementBatchRepository;

    // Task 10.1 - Dashboard data with today's stats and 30-day trend
    public DashboardData getDashboardData() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);

        // Get 30-day volume
        List<Transfer> recentTransfers = transferRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(thirtyDaysAgo.atStartOfDay()))
                .collect(Collectors.toList());

        BigDecimal volume30Day = recentTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Today's stats
        List<Transfer> todayTransfers = transferRepository.findByCreatedAtBetween(todayStart, todayEnd, Pageable.unpaged()).getContent();
        
        BigDecimal todayVolume = todayTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long todayCount = todayTransfers.size();
        long todaySuccessCount = todayTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .count();
        long todayFailedCount = todayTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.FAILED || t.getStatus() == TransferStatus.DECLINED)
                .count();

        // Get users count
        long usersCount = userRepository.count();

        // Get wallets count
        long walletsCount = walletRepository.count();
        
        // Get pending settlements count and total amount
        List<Transfer> pendingTransfers = transferRepository.findByStatus(TransferStatus.PENDING).stream().collect(Collectors.toList());
        long pendingSettlements = pendingTransfers.size();
        BigDecimal pendingSettlementAmount = pendingTransfers.stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get flagged transactions count
        long flaggedTransactions = fraudAlertRepository.countByStatus(FraudStatus.OPEN);

        return new DashboardData(volume30Day, usersCount, walletsCount, pendingSettlements, flaggedTransactions,
                todayVolume, todayCount, todaySuccessCount, todayFailedCount, pendingSettlementAmount);
    }

    // Get all users for admin user list with wallet balance
    public Page<UserWithWallet> getAllUsersWithWallet(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        List<UserWithWallet> userWithWallets = users.getContent().stream()
                .map(user -> {
                    Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);
                    return new UserWithWallet(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getPhone(),
                            user.getRole(),
                            user.isMaskingEnabled(),
                            user.getCreatedAt(),
                            user.getLockedUntil(),
                            wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                            wallet != null ? wallet.getStatus() : null
                    );
                })
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(userWithWallets, pageable, users.getTotalElements());
    }
    
    // Get all users for admin user list (deprecated - use getAllUsersWithWallet)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // Task 10.2 - Transaction monitoring with filters including amount range
    public Page<Transfer> getTransactionsWithFilters(Pageable pageable, String status, Long senderId, Long receiverId,
                                                     LocalDateTime startDate, LocalDateTime endDate, BigDecimal minAmount, BigDecimal maxAmount) {
        // Build dynamic query based on provided filters
        Page<Transfer> result;
        
        // Convert status string to enum if provided
        TransferStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = TransferStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore and return empty result
                return Page.empty(pageable);
            }
        }
        
        if (senderId != null) {
            // Filter by senderId with optional status and date range
            if (statusEnum != null) {
                result = transferRepository.findBySenderIdAndStatus(senderId, statusEnum, pageable);
            } else if (startDate != null && endDate != null) {
                result = transferRepository.findBySenderIdAndCreatedAtBetween(senderId, startDate, endDate, pageable);
            } else if (startDate != null) {
                result = transferRepository.findBySenderIdAndCreatedAtAfter(senderId, startDate, pageable);
            } else if (endDate != null) {
                result = transferRepository.findBySenderIdAndCreatedAtBefore(senderId, endDate, pageable);
            } else {
                result = transferRepository.findBySenderId(senderId, pageable);
            }
        } else if (receiverId != null) {
            // Filter by receiverId with optional status and date range
            if (statusEnum != null) {
                result = transferRepository.findByReceiverIdAndStatus(receiverId, statusEnum, pageable);
            } else if (startDate != null && endDate != null) {
                result = transferRepository.findByReceiverIdAndCreatedAtBetween(receiverId, startDate, endDate, pageable);
            } else if (startDate != null) {
                result = transferRepository.findByReceiverIdAndCreatedAtAfter(receiverId, startDate, pageable);
            } else if (endDate != null) {
                result = transferRepository.findByReceiverIdAndCreatedAtBefore(receiverId, endDate, pageable);
            } else {
                result = transferRepository.findByReceiverId(receiverId, pageable);
            }
        } else if (statusEnum != null) {
            // Filter by status only
            result = transferRepository.findByStatus(statusEnum, pageable);
        } else if (startDate != null && endDate != null) {
            // Filter by date range only
            result = transferRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else if (startDate != null) {
            result = transferRepository.findByCreatedAtAfter(startDate, pageable);
        } else if (endDate != null) {
            result = transferRepository.findByCreatedAtBefore(endDate, pageable);
        } else {
            result = transferRepository.findAll(pageable);
        }
        
        // Apply amount filters if provided (post-filtering since we can't easily add to all query methods)
        if (minAmount != null || maxAmount != null) {
            List<Transfer> filtered = result.getContent().stream()
                    .filter(t -> {
                        if (minAmount != null && t.getAmount().compareTo(minAmount) < 0) {
                            return false;
                        }
                        if (maxAmount != null && t.getAmount().compareTo(maxAmount) > 0) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            // Note: This is a simplified approach. For production, consider custom query with amount filtering
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, result.getTotalElements());
        }
        
        return result;
    }

    // Task 10.3 - Settlement monitoring with due date groupings
    public Page<Transfer> getPendingSettlements(Pageable pageable) {
        return transferRepository.findByStatus(TransferStatus.PENDING, pageable);
    }
    
    public SettlementSummary getSettlementSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime tomorrowEnd = LocalDate.now().plusDays(1).atTime(23, 59, 59);
        
        List<Transfer> allPending = transferRepository.findByStatus(TransferStatus.PENDING).stream().collect(Collectors.toList());
        
        // Overdue: Settlement date is BEFORE today (before 00:00 today)
        long overdue = allPending.stream()
                .filter(t -> t.getSettlementDate().isBefore(todayStart))
                .count();
        
        // Due today: Settlement date is anytime TODAY (00:00 to 23:59 today)
        long dueToday = allPending.stream()
                .filter(t -> !t.getSettlementDate().isBefore(todayStart) && 
                           (t.getSettlementDate().isBefore(todayEnd) || t.getSettlementDate().isEqual(todayEnd)))
                .count();
        
        // Due tomorrow: Settlement date is anytime TOMORROW
        long dueTomorrow = allPending.stream()
                .filter(t -> t.getSettlementDate().isAfter(todayEnd) && 
                           (t.getSettlementDate().isBefore(tomorrowEnd) || t.getSettlementDate().isEqual(tomorrowEnd)))
                .count();
        
        return new SettlementSummary(dueToday, dueTomorrow, overdue, allPending.size());
    }

    @Transactional
    public Transfer processSettlement(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new IllegalArgumentException("Transfer is not in PENDING status");
        }

        // Skip flagged transfers that require admin approval
        if (transfer.getAdminReviewStatus() == AdminReviewStatus.PENDING) {
            throw new IllegalArgumentException("Transfer requires admin approval before settlement");
        }

        // Admin can manually trigger settlement regardless of settlement date
        // This allows resolving system issues that caused delays

        Wallet senderWallet = walletRepository.findByUserId(transfer.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findByUserId(transfer.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        // Check receiver wallet status before settlement
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            // Receiver wallet is not active - follow decline/refund rules
            return refundTransferInternal(transfer, senderWallet, receiverWallet);
        }

        BigDecimal amount = transfer.getAmount();

        // Credit receiver wallet
        BigDecimal receiverOldBalance = receiverWallet.getBalance();
        BigDecimal receiverNewBalance = receiverOldBalance.add(amount);
        receiverWallet.setBalance(receiverNewBalance);

        // Create receiver CREDIT ledger entry
        LedgerEntry receiverLedgerEntry = LedgerEntry.builder()
                .transferId(transfer.getId())
                .userId(transfer.getReceiverId())
                .type(LedgerType.CREDIT)
                .amount(amount)
                .balanceBefore(receiverOldBalance)
                .balanceAfter(receiverNewBalance)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(receiverLedgerEntry);

        walletRepository.save(receiverWallet);

        // Update transfer status to SUCCESS
        transfer.setStatus(TransferStatus.SUCCESS);
        transferRepository.save(transfer);

        // Get sender and receiver names for notifications
        User sender = userRepository.findById(transfer.getSenderId()).orElse(null);
        User receiver = userRepository.findById(transfer.getReceiverId()).orElse(null);
        String senderName = sender != null ? sender.getName() : "User #" + transfer.getSenderId();
        String receiverName = receiver != null ? receiver.getName() : "User #" + transfer.getReceiverId();

        // Create notifications for both sender and receiver
        Notification senderNotification = Notification.builder()
                .userId(transfer.getSenderId())
                .type(NotificationType.SETTLEMENT)
                .message("₹" + amount + " transfer to " + receiverName + " has been settled")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(senderNotification);

        Notification receiverNotification = Notification.builder()
                .userId(transfer.getReceiverId())
                .type(NotificationType.SETTLEMENT)
                .message("₹" + amount + " received from " + senderName)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(receiverNotification);

        return transfer;
    }

    // Task 10.4 - Fraud review
    public Page<FraudAlert> getFlaggedTransactions(Pageable pageable) {
        Page<FraudAlert> openAlerts = fraudAlertRepository.findByStatus(FraudStatus.OPEN, pageable);
        return openAlerts;
    }

    @Transactional
    public Transfer approveTransaction(Long fraudAlertId) {
        FraudAlert fraudAlert = fraudAlertRepository.findById(fraudAlertId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud alert not found"));
        
        Long transferId = fraudAlert.getTransferId();
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (transfer.getAdminReviewStatus() != AdminReviewStatus.PENDING) {
            throw new IllegalArgumentException("Transfer is not pending admin review");
        }

        // Update fraud alert
        fraudAlert.setStatus(FraudStatus.APPROVED);
        fraudAlert.setReviewedAt(LocalDateTime.now());
        fraudAlertRepository.save(fraudAlert);

        transfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
        transferRepository.save(transfer);

        return transfer;
    }

    @Transactional
    public Transfer refundTransaction(Long fraudAlertId) {
        FraudAlert fraudAlert = fraudAlertRepository.findById(fraudAlertId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud alert not found"));
        
        Long transferId = fraudAlert.getTransferId();
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        // Check if already refunded (idempotency) - MUST be checked before pending review validation
        if (transfer.getAdminReviewStatus() == AdminReviewStatus.REFUNDED) {
            return transfer;
        }

        if (transfer.getAdminReviewStatus() != AdminReviewStatus.PENDING) {
            throw new IllegalArgumentException("Transfer is not pending admin review");
        }

        Wallet senderWallet = walletRepository.findByUserId(transfer.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));

        BigDecimal amount = transfer.getAmount();

        return refundTransferInternal(transfer, senderWallet, null);
    }

    private Transfer refundTransferInternal(Transfer transfer, Wallet senderWallet, Wallet receiverWallet) {
        BigDecimal amount = transfer.getAmount();

        // Credit sender wallet (refund)
        BigDecimal senderOldBalance = senderWallet.getBalance();
        BigDecimal senderNewBalance = senderOldBalance.add(amount);
        senderWallet.setBalance(senderNewBalance);

        // Decrease todaySpending for refund
        BigDecimal newTodaySpending = senderWallet.getTodaySpending().subtract(amount);
        if (newTodaySpending.compareTo(BigDecimal.ZERO) < 0) {
            newTodaySpending = BigDecimal.ZERO;
        }
        senderWallet.setTodaySpending(newTodaySpending);

        // Create sender CREDIT (refund) ledger entry
        LedgerEntry refundLedgerEntry = LedgerEntry.builder()
                .transferId(transfer.getId())
                .userId(transfer.getSenderId())
                .type(LedgerType.CREDIT)
                .amount(amount)
                .balanceBefore(senderOldBalance)
                .balanceAfter(senderNewBalance)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(refundLedgerEntry);

        walletRepository.save(senderWallet);

        // Update transfer status
        transfer.setStatus(TransferStatus.DECLINED);
        transfer.setAdminReviewStatus(AdminReviewStatus.REFUNDED);
        transferRepository.save(transfer);

        // Update fraud alert
        FraudAlert fraudAlert = fraudAlertRepository.findByTransferId(transfer.getId()).orElse(null);
        if (fraudAlert != null) {
            fraudAlert.setStatus(FraudStatus.RESOLVED);
            fraudAlert.setReviewedAt(LocalDateTime.now());
            fraudAlertRepository.save(fraudAlert);
        }

        // Get receiver name for notification
        User receiver = userRepository.findById(transfer.getReceiverId()).orElse(null);
        String receiverName = receiver != null ? receiver.getName() : "User #" + transfer.getReceiverId();
        
        // Create notification only for sender (receiver doesn't need to know about failed transfers)
        Notification senderNotification = Notification.builder()
                .userId(transfer.getSenderId())
                .type(NotificationType.REFUND)
                .message("₹" + amount + " transfer to " + receiverName + " failed. Amount refunded to your wallet")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(senderNotification);

        return transfer;
    }

    // Task 10.5 - Job execution history
    public List<ScheduledJobLog> getJobExecutionHistory(String jobName) {
        if (jobName != null) {
            return scheduledJobLogRepository.findByJobNameOrderByExecutionTimeDesc(jobName);
        }
        return scheduledJobLogRepository.findAll();
    }

    // Task 10.6 - Reconciliation with today's mismatch detection
    public DailySummary getDailyReconciliation(LocalDate date) {
        return dailySummaryRepository.findBySummaryDate(date)
                .orElseThrow(() -> new IllegalArgumentException("Daily summary not found for date: " + date));
    }

    public List<DailySummary> getReconciliationReport(LocalDate startDate, LocalDate endDate) {
        return dailySummaryRepository.findBySummaryDateBetween(startDate, endDate);
    }
    
    public ReconciliationStatus getTodayReconciliation() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        
        // Only get ledger entries for SUCCESSFUL transfers (exclude deposits and refunds)
        // Get all successful transfers from today
        List<Transfer> todaySuccessfulTransfers = transferRepository.findByCreatedAtBetween(todayStart, todayEnd, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .collect(Collectors.toList());
        
        // Get the transfer IDs
        List<Long> successfulTransferIds = todaySuccessfulTransfers.stream()
                .map(Transfer::getId)
                .collect(Collectors.toList());
        
        // Get all ledger entries for today
        List<LedgerEntry> todayEntries = ledgerEntryRepository.findByCreatedAtBetween(todayStart, todayEnd);
        
        // Filter to only include entries for successful transfers
        List<LedgerEntry> transferEntries = todayEntries.stream()
                .filter(e -> e.getTransferId() != null && successfulTransferIds.contains(e.getTransferId()))
                .collect(Collectors.toList());
        
        BigDecimal totalDebits = transferEntries.stream()
                .filter(e -> e.getType() == LedgerType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = transferEntries.stream()
                .filter(e -> e.getType() == LedgerType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long debitCount = transferEntries.stream()
                .filter(e -> e.getType() == LedgerType.DEBIT)
                .count();
        
        long creditCount = transferEntries.stream()
                .filter(e -> e.getType() == LedgerType.CREDIT)
                .count();
        
        // Get pending settlement amount for today's transfers
        List<Transfer> todayPendingTransfers = transferRepository.findByCreatedAtBetween(todayStart, todayEnd, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(t -> t.getStatus() == TransferStatus.PENDING)
                .collect(Collectors.toList());
        
        BigDecimal pendingSettlement = todayPendingTransfers.stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long pendingCount = todayPendingTransfers.size();
        
        boolean balanced = totalDebits.compareTo(totalCredits) == 0;
        BigDecimal mismatch = totalDebits.subtract(totalCredits);
        
        return new ReconciliationStatus(totalDebits, totalCredits, debitCount, creditCount, 
                balanced, mismatch, pendingSettlement, pendingCount);
    }
    
    // Get user detail with wallet and recent transactions
    public UserDetail getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        
        // Get recent 10 transfers (sent and received)
        List<Transfer> recentTransfers = transferRepository.findAll().stream()
                .filter(t -> t.getSenderId().equals(userId) || t.getReceiverId().equals(userId))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());
        
        return new UserDetail(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isMaskingEnabled(),
                user.getCreatedAt(),
                user.getLockedUntil(),
                wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getStatus() : null,
                recentTransfers
        );
    }
    
    // Freeze user wallet (set to FROZEN status)
    @Transactional
    public void freezeUserWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user"));
        
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new IllegalArgumentException("Wallet is already frozen");
        }
        
        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(wallet);
        
        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(userId)
                .action("WALLET_FROZEN")
                .entityType("WALLET")
                .entityId(userId.toString())
                .details("Admin froze wallet for user ID: " + userId)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
    
    // Unfreeze user wallet (set to ACTIVE status)
    @Transactional
    public void unfreezeUserWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user"));
        
        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new IllegalArgumentException("Wallet is not frozen");
        }
        
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        
        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(userId)
                .action("WALLET_UNFROZEN")
                .entityType("WALLET")
                .entityId(userId.toString())
                .details("Admin unfroze wallet for user ID: " + userId)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    // Response DTOs
    public static class DashboardData {
        private final BigDecimal volume30Day;
        private final long usersCount;
        private final long walletsCount;
        private final long pendingSettlements;
        private final long flaggedTransactions;
        private final BigDecimal todayVolume;
        private final long todayCount;
        private final long todaySuccessCount;
        private final long todayFailedCount;
        private final BigDecimal pendingSettlementAmount;

        public DashboardData(BigDecimal volume30Day, long usersCount, long walletsCount, 
                           long pendingSettlements, long flaggedTransactions,
                           BigDecimal todayVolume, long todayCount, long todaySuccessCount, long todayFailedCount,
                           BigDecimal pendingSettlementAmount) {
            this.volume30Day = volume30Day;
            this.usersCount = usersCount;
            this.walletsCount = walletsCount;
            this.pendingSettlements = pendingSettlements;
            this.flaggedTransactions = flaggedTransactions;
            this.todayVolume = todayVolume;
            this.todayCount = todayCount;
            this.todaySuccessCount = todaySuccessCount;
            this.todayFailedCount = todayFailedCount;
            this.pendingSettlementAmount = pendingSettlementAmount;
        }

        public BigDecimal getVolume30Day() { return volume30Day; }
        public long getUsersCount() { return usersCount; }
        public long getWalletsCount() { return walletsCount; }
        public long getPendingSettlements() { return pendingSettlements; }
        public long getFlaggedTransactions() { return flaggedTransactions; }
        public BigDecimal getTodayVolume() { return todayVolume; }
        public long getTodayCount() { return todayCount; }
        public long getTodaySuccessCount() { return todaySuccessCount; }
        public long getTodayFailedCount() { return todayFailedCount; }
        public BigDecimal getPendingSettlementAmount() { return pendingSettlementAmount; }
    }
    
    public static class SettlementSummary {
        private final long dueToday;
        private final long dueTomorrow;
        private final long overdue;
        private final long totalPending;
        
        public SettlementSummary(long dueToday, long dueTomorrow, long overdue, long totalPending) {
            this.dueToday = dueToday;
            this.dueTomorrow = dueTomorrow;
            this.overdue = overdue;
            this.totalPending = totalPending;
        }
        
        public long getDueToday() { return dueToday; }
        public long getDueTomorrow() { return dueTomorrow; }
        public long getOverdue() { return overdue; }
        public long getTotalPending() { return totalPending; }
    }
    
    public static class ReconciliationStatus {
        private final BigDecimal totalDebits;
        private final BigDecimal totalCredits;
        private final long debitCount;
        private final long creditCount;
        private final boolean balanced;
        private final BigDecimal mismatch;
        private final BigDecimal pendingSettlement;
        private final long pendingCount;
        
        public ReconciliationStatus(BigDecimal totalDebits, BigDecimal totalCredits, 
                                   long debitCount, long creditCount, boolean balanced, BigDecimal mismatch,
                                   BigDecimal pendingSettlement, long pendingCount) {
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
            this.debitCount = debitCount;
            this.creditCount = creditCount;
            this.balanced = balanced;
            this.mismatch = mismatch;
            this.pendingSettlement = pendingSettlement;
            this.pendingCount = pendingCount;
        }
        
        public BigDecimal getTotalDebits() { return totalDebits; }
        public BigDecimal getTotalCredits() { return totalCredits; }
        public long getDebitCount() { return debitCount; }
        public long getCreditCount() { return creditCount; }
        public boolean isBalanced() { return balanced; }
        public BigDecimal getMismatch() { return mismatch; }
        public BigDecimal getPendingSettlement() { return pendingSettlement; }
        public long getPendingCount() { return pendingCount; }
    }
    
    public static class UserWithWallet {
        private final Long id;
        private final String name;
        private final String email;
        private final String phone;
        private final Role role;
        private final boolean maskingEnabled;
        private final LocalDateTime createdAt;
        private final LocalDateTime lockedUntil;
        private final BigDecimal walletBalance;
        private final WalletStatus walletStatus;
        
        public UserWithWallet(Long id, String name, String email, String phone, Role role, 
                             boolean maskingEnabled, LocalDateTime createdAt, LocalDateTime lockedUntil,
                             BigDecimal walletBalance, WalletStatus walletStatus) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.maskingEnabled = maskingEnabled;
            this.createdAt = createdAt;
            this.lockedUntil = lockedUntil;
            this.walletBalance = walletBalance;
            this.walletStatus = walletStatus;
        }
        
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public Role getRole() { return role; }
        public boolean isMaskingEnabled() { return maskingEnabled; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLockedUntil() { return lockedUntil; }
        public BigDecimal getWalletBalance() { return walletBalance; }
        public WalletStatus getWalletStatus() { return walletStatus; }
    }
    
    public static class UserDetail {
        private final Long id;
        private final String name;
        private final String email;
        private final String phone;
        private final Role role;
        private final boolean maskingEnabled;
        private final LocalDateTime createdAt;
        private final LocalDateTime lockedUntil;
        private final BigDecimal walletBalance;
        private final WalletStatus walletStatus;
        private final List<Transfer> recentTransfers;
        
        public UserDetail(Long id, String name, String email, String phone, Role role,
                         boolean maskingEnabled, LocalDateTime createdAt, LocalDateTime lockedUntil,
                         BigDecimal walletBalance, WalletStatus walletStatus, List<Transfer> recentTransfers) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.maskingEnabled = maskingEnabled;
            this.createdAt = createdAt;
            this.lockedUntil = lockedUntil;
            this.walletBalance = walletBalance;
            this.walletStatus = walletStatus;
            this.recentTransfers = recentTransfers;
        }
        
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public Role getRole() { return role; }
        public boolean isMaskingEnabled() { return maskingEnabled; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLockedUntil() { return lockedUntil; }
        public BigDecimal getWalletBalance() { return walletBalance; }
        public WalletStatus getWalletStatus() { return walletStatus; }
        public List<Transfer> getRecentTransfers() { return recentTransfers; }
    }
}
