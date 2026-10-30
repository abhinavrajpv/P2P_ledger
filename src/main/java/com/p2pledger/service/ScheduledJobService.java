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
    @Scheduled(fixedDelay = 3600000) // Run every hour, wait for previous execution to complete
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

        try {
            // Find all PENDING transfers whose settlement date has arrived
            List<Transfer> eligibleTransfers = transferRepository.findAll().stream()
                    .filter(t -> t.getStatus() == TransferStatus.PENDING)
                    .filter(t -> t.getSettlementDate().isBefore(LocalDateTime.now()) || t.getSettlementDate().equals(LocalDateTime.now()))
                    .filter(t -> t.getAdminReviewStatus() != AdminReviewStatus.PENDING)
                    .toList();

            for (Transfer transfer : eligibleTransfers) {
                try {
                    settleTransferInternal(transfer);
                    processedCount++;
                } catch (Exception e) {
                    failureCount++;
                    // Log error but continue processing other transfers
                }
            }

            batch.setStatus(SettlementBatchStatus.SUCCESS);
            batch.setCompletedAt(LocalDateTime.now());
            batch.setProcessedCount(processedCount);
            batch.setFailureCount(failureCount);
            batch.setDetails("Processed " + processedCount + " transfers, " + failureCount + " failures");
        } catch (Exception e) {
            batch.setStatus(SettlementBatchStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now());
            batch.setDetails("Error: " + e.getMessage());
        }

        settlementBatchRepository.save(batch);

        // Log execution
        String details = String.format("{\"batchId\":\"%s\",\"processedCount\":%d,\"failureCount\":%d}",
                batchId, processedCount, failureCount);
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
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
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

                // Log audit event
                createAuditLog(user.getId(), "ACCOUNT_UNLOCKED", "User", user.getId().toString(),
                        "Account unlocked after lockout period expired");
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

    // Task 11.4 - Daily summary job
    @Scheduled(cron = "0 0 23 * * ?") // Run daily at 11 PM
    @Transactional
    public void generateDailySummary() {
        LocalDate summaryDate = LocalDate.now();

        // Check if summary already exists for today
        if (dailySummaryRepository.findBySummaryDate(summaryDate).isPresent()) {
            return; // Skip if already generated today
        }

        String batchId = "DAILY_SUMMARY-" + summaryDate.toString();

        // Calculate summary statistics
        LocalDateTime startOfDay = summaryDate.atStartOfDay();
        LocalDateTime endOfDay = summaryDate.atTime(23, 59, 59);

        List<Transfer> transfers = transferRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfDay))
                .filter(t -> t.getCreatedAt().isBefore(endOfDay))
                .toList();

        long totalTransfers = transfers.size();
        long successfulTransfers = transfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .count();
        long failedTransfers = transfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.FAILED || t.getStatus() == TransferStatus.DECLINED)
                .count();

        BigDecimal totalAmount = transfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            DailySummary summary = DailySummary.builder()
                    .summaryDate(summaryDate)
                    .totalTransfers(totalTransfers)
                    .totalAmount(totalAmount)
                    .successfulTransfers(successfulTransfers)
                    .failedTransfers(failedTransfers)
                    .build();
            dailySummaryRepository.save(summary);

            // Log execution
            String details = String.format("{\"batchId\":\"%s\",\"date\":\"%s\",\"totalTransfers\":%d,\"successfulTransfers\":%d,\"failedTransfers\":%d,\"totalAmount\":\"%s\"}",
                    batchId, summaryDate, totalTransfers, successfulTransfers, failedTransfers, totalAmount);
            ScheduledJobLog log = ScheduledJobLog.builder()
                    .jobName("generate_daily_summary")
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
                    .jobName("generate_daily_summary")
                    .executionTime(LocalDateTime.now())
                    .status(ScheduledJobStatus.FAILED)
                    .details(details)
                    .build();
            scheduledJobLogRepository.save(log);
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
                .build();
        ledgerEntryRepository.save(receiverLedgerEntry);

        walletRepository.save(receiverWallet);

        // Update transfer status to SUCCESS
        transfer.setStatus(TransferStatus.SUCCESS);
        transferRepository.save(transfer);

        // Create notification to both sender and receiver
        createNotification(transfer.getSenderId(), NotificationType.SETTLEMENT,
                "Transfer of ₹" + amount + " settled successfully");
        createNotification(transfer.getReceiverId(), NotificationType.SETTLEMENT,
                "Received ₹" + amount + " from " + transfer.getSenderId());
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
                .build();
        ledgerEntryRepository.save(refundLedgerEntry);

        walletRepository.save(senderWallet);

        // If receiver wallet was provided, also refund the receiver
        if (receiverWallet != null) {
            BigDecimal receiverOldBalance = receiverWallet.getBalance();
            BigDecimal receiverNewBalance = receiverOldBalance.add(amount);
            receiverWallet.setBalance(receiverNewBalance);

            LedgerEntry receiverLedgerEntry = LedgerEntry.builder()
                    .transferId(transfer.getId())
                    .userId(transfer.getReceiverId())
                    .type(LedgerType.CREDIT)
                    .amount(amount)
                    .balanceBefore(receiverOldBalance)
                    .balanceAfter(receiverNewBalance)
                    .build();
            ledgerEntryRepository.save(receiverLedgerEntry);

            walletRepository.save(receiverWallet);
        }

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

        createNotification(transfer.getSenderId(), NotificationType.REFUND,
                "Transfer of ₹" + amount + " refunded: receiver wallet not active");
        createNotification(transfer.getReceiverId(), NotificationType.REFUND,
                "Transfer of ₹" + amount + " to you was refunded: receiver wallet not active");
    }

    private void createNotification(Long userId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .isRead(false)
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
                .build();
        auditLogRepository.save(auditLog);
    }

    // Task 10.5 - Job execution history (delegating to repository)
    public java.util.List<ScheduledJobLog> getJobExecutionHistory(String jobName) {
        if (jobName != null) {
            return scheduledJobLogRepository.findByJobNameOrderByExecutionTimeDesc(jobName);
        }
        return scheduledJobLogRepository.findAll();
    }

    public java.util.List<ScheduledJobLog> getAllJobExecutionHistory() {
        return scheduledJobLogRepository.findAll();
    }
}
