package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledJobService {

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

    // Task 11.1 - Hourly settlement job
    @Scheduled(cron = "0 0 * * * ?") // Run every hour at the top of the hour
    @Transactional
    public void settleEligibleTransfers() {
        String batchId = "SETTLEMENT-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();

        SettlementBatch batch = SettlementBatch.builder()
                .batchId(batchId)
                .startedAt(LocalDateTime.now())
                .status(SettlementBatchStatus.RUNNING)
                .processedCount(0)
                .failureCount(0)
                .details("Starting settlement batch")
                .build();
        settlementBatchRepository.save(batch);

        int processedCount = 0;
        int failureCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            // Find all PENDING transfers whose settlement date has arrived
            // Note: Include ALL pending transfers, even those awaiting admin review
            // If admin doesn't take action by settlement date, transaction settles automatically
            List<Transfer> eligibleTransfers = transferRepository.findAll().stream()
                    .filter(t -> t.getStatus() == TransferStatus.PENDING)
                    .filter(t -> t.getSettlementDate().isBefore(LocalDateTime.now()) || t.getSettlementDate().equals(LocalDateTime.now()))
                    .toList();

            for (Transfer transfer : eligibleTransfers) {
                try {
                    // Auto-approve if admin review is still pending at settlement time
                    if (transfer.getAdminReviewStatus() == AdminReviewStatus.PENDING) {
                        transfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
                        transferRepository.save(transfer);
                        
                        // Update fraud alert status to APPROVED (auto-approved by system)
                        FraudAlert fraudAlert = fraudAlertRepository.findByTransferId(transfer.getId()).orElse(null);
                        if (fraudAlert != null && fraudAlert.getStatus() == FraudStatus.OPEN) {
                            fraudAlert.setStatus(FraudStatus.APPROVED);
                            fraudAlert.setReviewedAt(LocalDateTime.now());
                            fraudAlertRepository.save(fraudAlert);
                        }
                    }
                    
                    settleTransferInternal(transfer);
                    processedCount++;
                    totalAmount = totalAmount.add(transfer.getAmount());
                } catch (Exception e) {
                    failureCount++;
                    // Log error but continue processing other transfers
                }
            }

            batch.setStatus(SettlementBatchStatus.SUCCESS);
            batch.setCompletedAt(LocalDateTime.now());
            batch.setProcessedCount(processedCount);
            batch.setFailureCount(failureCount);
            batch.setDetails("Processed " + processedCount + " transfers, " + failureCount + " failures, total amount: " + totalAmount);
        } catch (Exception e) {
            batch.setStatus(SettlementBatchStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now());
            batch.setDetails("Error: " + e.getMessage());
        }

        settlementBatchRepository.save(batch);

        // Log execution with settled count, failed count, and total amount
        String details = String.format("{\"batchId\":\"%s\",\"processedCount\":%d,\"failureCount\":%d,\"totalAmount\":\"%s\"}",
                batchId, processedCount, failureCount, totalAmount.toString());
        ScheduledJobLog log = ScheduledJobLog.builder()
                .jobName("settle_eligible_transfers")
                .executionTime(LocalDateTime.now())
                .status(ScheduledJobStatus.SUCCESS)
                .details(details)
                .batchId(batchId)
                .build();
        scheduledJobLogRepository.save(log);
    }

    // Task 11.2 - Daily limit reset job
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void resetDailyLimits() {
        String batchId = "LIMIT_RESET-" + LocalDate.now().toString();

        int resetCount = 0;
        int walletCount = 0;

        try {
            List<Wallet> wallets = walletRepository.findAll();

            for (Wallet wallet : wallets) {
                // Reset todaySpending to zero
                wallet.setTodaySpending(BigDecimal.ZERO);
                // Update dailyLimitResetDate to today
                wallet.setDailyLimitResetDate(LocalDate.now());
                walletRepository.save(wallet);
                resetCount++;
            }

            walletCount = wallets.size();
        } catch (Exception e) {
            // Log error but continue
        }

        // Log execution
        String details = String.format("{\"batchId\":\"%s\",\"walletsProcessed\":%d,\"todaySpendingReset\":%d}",
                batchId, walletCount, resetCount);
        ScheduledJobLog log = ScheduledJobLog.builder()
                .jobName("reset_daily_limits")
                .executionTime(LocalDateTime.now())
                .status(ScheduledJobStatus.SUCCESS)
                .details(details)
                .batchId(batchId)
                .build();
        scheduledJobLogRepository.save(log);
    }

    // Task 11.3 - Account unlock job
    @Scheduled(cron = "0 */5 * * * ?") // Run every 5 minutes
    @Transactional
    public void unlockLockedAccounts() {
        String batchId = "ACCOUNT_UNLOCK-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();

        int unlockedCount = 0;
        int checkedCount = 0;

        try {
            List<User> lockedUsers = userRepository.findAll().stream()
                    .filter(u -> u.getLockedUntil() != null)
                    .filter(u -> u.getLockedUntil().isBefore(LocalDateTime.now()))
                    .toList();

            for (User user : lockedUsers) {
                user.setLockedUntil(null);
                user.setFailedLoginCount(0);
                userRepository.save(user);
                unlockedCount++;
            }

            checkedCount = lockedUsers.size();
        } catch (Exception e) {
            // Log error but continue
        }

        // Log execution
        String details = String.format("{\"batchId\":\"%s\",\"accountsChecked\":%d,\"accountsUnlocked\":%d}",
                batchId, checkedCount, unlockedCount);
        ScheduledJobLog log = ScheduledJobLog.builder()
                .jobName("unlock_locked_accounts")
                .executionTime(LocalDateTime.now())
                .status(ScheduledJobStatus.SUCCESS)
                .details(details)
                .batchId(batchId)
                .build();
        scheduledJobLogRepository.save(log);
    }

    // Task 11.4 - Daily summary and reconciliation job (combined)
    @Scheduled(cron = "0 */5 * * * ?") // Run every 5 minutes
    @Transactional
    public void generateDailySummaryAndReconciliation() {
        LocalDate summaryDate = LocalDate.now();

        // Check if summary already exists for today - if so, update it instead of creating new
        java.util.Optional<DailySummary> existingSummary = dailySummaryRepository.findBySummaryDate(summaryDate);
        boolean isUpdate = existingSummary.isPresent();

        String batchId = "DAILY_SUMMARY_RECON-" + summaryDate.toString();

        // Calculate summary statistics
        LocalDateTime startOfDay = summaryDate.atStartOfDay();
        LocalDateTime endOfDay = summaryDate.atTime(23, 59, 59);

        // Get all transfers initiated today
        List<Transfer> initiatedTransfers = transferRepository.findByCreatedAtBetween(startOfDay, endOfDay, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        // 1. Total number of transfers initiated and their total amount
        long totalTransfersInitiated = initiatedTransfers.size();
        BigDecimal totalAmountInitiated = initiatedTransfers.stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Total number of transfers settled and their total amount
        long totalTransfersSettled = initiatedTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .count();
        BigDecimal totalAmountSettled = initiatedTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Number of fraud alerts raised today
        long fraudAlertsRaised = fraudAlertRepository.findAll().stream()
                .filter(fa -> fa.getCreatedAt().isAfter(startOfDay) && fa.getCreatedAt().isBefore(endOfDay))
                .count();

        // 4. Number of new user registrations today
        long newUserRegistrations = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(startOfDay) && u.getCreatedAt().isBefore(endOfDay))
                .count();

        // 5. Total pending settlement amount across all users (today's pending transfers)
        List<Transfer> pendingTransfers = initiatedTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.PENDING)
                .toList();
        long pendingSettlementCount = pendingTransfers.size();
        BigDecimal pendingSettlementAmount = pendingTransfers.stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // RECONCILIATION: Get ledger entries for successful transfers only
        List<Transfer> successfulTransfersForRecon = initiatedTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .toList();
        
        List<Long> successfulTransferIds = successfulTransfersForRecon.stream()
                .map(Transfer::getId)
                .toList();
        
        // Get all ledger entries for today
        List<LedgerEntry> todayEntries = ledgerEntryRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        // Filter to only include entries for successful transfers (excludes deposits and refunds)
        List<LedgerEntry> transferEntries = todayEntries.stream()
                .filter(e -> e.getTransferId() != null && successfulTransferIds.contains(e.getTransferId()))
                .toList();
        
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
        
        boolean reconciliationBalanced = totalDebits.compareTo(totalCredits) == 0;
        BigDecimal reconciliationMismatch = totalDebits.subtract(totalCredits);

        try {
            DailySummary summary;
            if (isUpdate) {
                // Update existing summary
                summary = existingSummary.get();
                summary.setTotalTransfersInitiated(totalTransfersInitiated);
                summary.setTotalAmountInitiated(totalAmountInitiated);
                summary.setTotalTransfersSettled(totalTransfersSettled);
                summary.setTotalAmountSettled(totalAmountSettled);
                summary.setFraudAlertsRaised(fraudAlertsRaised);
                summary.setNewUserRegistrations(newUserRegistrations);
                summary.setPendingSettlementCount(pendingSettlementCount);
                summary.setPendingSettlementAmount(pendingSettlementAmount);
                summary.setTotalDebits(totalDebits);
                summary.setTotalCredits(totalCredits);
                summary.setDebitCount(debitCount);
                summary.setCreditCount(creditCount);
                summary.setReconciliationBalanced(reconciliationBalanced);
                summary.setReconciliationMismatch(reconciliationMismatch);
                summary.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new summary
                summary = DailySummary.builder()
                        .summaryDate(summaryDate)
                        // New fields
                        .totalTransfersInitiated(totalTransfersInitiated)
                        .totalAmountInitiated(totalAmountInitiated)
                        .totalTransfersSettled(totalTransfersSettled)
                        .totalAmountSettled(totalAmountSettled)
                        .fraudAlertsRaised(fraudAlertsRaised)
                        .newUserRegistrations(newUserRegistrations)
                        .pendingSettlementCount(pendingSettlementCount)
                        .pendingSettlementAmount(pendingSettlementAmount)
                        // Reconciliation fields
                        .totalDebits(totalDebits)
                        .totalCredits(totalCredits)
                        .debitCount(debitCount)
                        .creditCount(creditCount)
                        .reconciliationBalanced(reconciliationBalanced)
                        .reconciliationMismatch(reconciliationMismatch)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
            dailySummaryRepository.save(summary);

            // If reconciliation is not balanced, create a critical notification for all admins
            if (!reconciliationBalanced) {
                createAdminNotificationsForMismatch(summaryDate, reconciliationMismatch);
            }

            // Log execution
            String details = String.format("{\"batchId\":\"%s\",\"date\":\"%s\",\"initiated\":%d,\"settled\":%d,\"fraudAlerts\":%d,\"newUsers\":%d,\"pendingAmount\":\"%s\",\"reconciliationBalanced\":%s,\"mismatch\":\"%s\"}",
                    batchId, summaryDate, totalTransfersInitiated, totalTransfersSettled, fraudAlertsRaised, newUserRegistrations, 
                    pendingSettlementAmount, reconciliationBalanced, reconciliationMismatch);
            ScheduledJobLog log = ScheduledJobLog.builder()
                    .jobName("daily_summary_reconciliation")
                    .executionTime(LocalDateTime.now())
                    .status(ScheduledJobStatus.SUCCESS)
                    .details(details)
                    .batchId(batchId)
                    .build();
            scheduledJobLogRepository.save(log);
        } catch (Exception e) {
            // Log execution failure
            String details = String.format("{\"batchId\":\"%s\",\"error\":\"%s\"}", batchId, e.getMessage());
            ScheduledJobLog log = ScheduledJobLog.builder()
                    .jobName("daily_summary_reconciliation")
                    .executionTime(LocalDateTime.now())
                    .status(ScheduledJobStatus.FAILED)
                    .details(details)
                    .build();
            scheduledJobLogRepository.save(log);
        }
    }

    // Helper method to create notifications for all admin users when reconciliation mismatch is detected
    private void createAdminNotificationsForMismatch(LocalDate date, BigDecimal mismatch) {
        // Get all admin users
        List<User> adminUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .toList();

        for (User admin : adminUsers) {
            Notification notification = Notification.builder()
                    .userId(admin.getId())
                    .type(NotificationType.SYSTEM)
                    .message("⚠️ CRITICAL: Reconciliation mismatch detected for " + date + ". Mismatch amount: ₹" + mismatch + ". Please review immediately.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }
    }

    // Internal settlement method
    private void settleTransferInternal(Transfer transfer) {
        Wallet senderWallet = walletRepository.findByUserId(transfer.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findByUserId(transfer.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        // Check receiver wallet status before settlement
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            // Receiver wallet is not active - follow decline/refund rules
            refundTransferInternal(transfer, senderWallet, receiverWallet);
            return;
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

        // Create notification to both sender and receiver
        System.out.println("DEBUG: Creating settlement notifications for transfer ID: " + transfer.getId());
        System.out.println("DEBUG: Sender ID: " + transfer.getSenderId() + ", Receiver ID: " + transfer.getReceiverId());
        
        createNotification(transfer.getSenderId(), NotificationType.SETTLEMENT,
                "₹" + amount + " transfer to " + receiverName + " has been settled");
        createNotification(transfer.getReceiverId(), NotificationType.SETTLEMENT,
                "₹" + amount + " received from " + senderName);
        
        System.out.println("DEBUG: Settlement notifications created successfully");
    }

    // Internal refund method (reused for settlement failures)
    private void refundTransferInternal(Transfer transfer, Wallet senderWallet, Wallet receiverWallet) {
        // Check if already refunded (idempotency)
        if (transfer.getAdminReviewStatus() == AdminReviewStatus.REFUNDED) {
            return;
        }

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

        // Update transfer status to DECLINED (refund completed)
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
        createNotification(transfer.getSenderId(), NotificationType.REFUND,
                "₹" + amount + " transfer to " + receiverName + " failed. Amount refunded to your wallet");
    }

    private void createNotification(Long userId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private void createAuditLog(Long actorUserId, String action, String entityType, String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actorUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    // Task 10.5 - Job execution history (delegating to repository) - Last 20 runs
    public java.util.List<ScheduledJobLog> getJobExecutionHistory(String jobName) {
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 20);
        
        if (jobName != null && !jobName.trim().isEmpty()) {
            return scheduledJobLogRepository.findTop20ByJobNameOrderByExecutionTimeDesc(jobName, pageable);
        }
        return scheduledJobLogRepository.findTop20ByOrderByExecutionTimeDesc(pageable);
    }

    public java.util.List<ScheduledJobLog> getAllJobExecutionHistory() {
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, 20);
        return scheduledJobLogRepository.findTop20ByOrderByExecutionTimeDesc(pageable);
    }
}
