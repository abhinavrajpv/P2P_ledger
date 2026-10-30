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

    // Task 10.1 - Dashboard data
    public DashboardData getDashboardData() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Get 30-day volume
        List<Transfer> recentTransfers = transferRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(thirtyDaysAgo.atStartOfDay()))
                .collect(Collectors.toList());

        BigDecimal volume30Day = recentTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get users count
        long usersCount = userRepository.count();

        // Get wallets count
        long walletsCount = walletRepository.count();

        return new DashboardData(volume30Day, usersCount, walletsCount);
    }

    // Task 10.2 - Transaction monitoring with filters
    public Page<Transfer> getTransactionsWithFilters(Pageable pageable, String status, Long senderId, Long receiverId,
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        // Build dynamic query based on provided filters
        if (senderId != null) {
            // Filter by senderId with optional status and date range
            if (status != null) {
                return transferRepository.findBySenderIdAndStatus(senderId, status, pageable);
            }
            if (startDate != null && endDate != null) {
                return transferRepository.findBySenderIdAndCreatedAtBetween(senderId, startDate, endDate, pageable);
            }
            if (startDate != null) {
                return transferRepository.findBySenderIdAndCreatedAtAfter(senderId, startDate, pageable);
            }
            if (endDate != null) {
                return transferRepository.findBySenderIdAndCreatedAtBefore(senderId, endDate, pageable);
            }
            return transferRepository.findBySenderId(senderId, pageable);
        } else if (receiverId != null) {
            // Filter by receiverId with optional status and date range
            if (status != null) {
                return transferRepository.findByReceiverIdAndStatus(receiverId, status, pageable);
            }
            if (startDate != null && endDate != null) {
                return transferRepository.findByReceiverIdAndCreatedAtBetween(receiverId, startDate, endDate, pageable);
            }
            if (startDate != null) {
                return transferRepository.findByReceiverIdAndCreatedAtAfter(receiverId, startDate, pageable);
            }
            if (endDate != null) {
                return transferRepository.findByReceiverIdAndCreatedAtBefore(receiverId, endDate, pageable);
            }
            return transferRepository.findByReceiverId(receiverId, pageable);
        } else if (status != null) {
            // Filter by status only
            return transferRepository.findByStatus(status, pageable);
        } else if (startDate != null && endDate != null) {
            // Filter by date range only
            return transferRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else if (startDate != null) {
            return transferRepository.findByCreatedAtAfter(startDate, pageable);
        } else if (endDate != null) {
            return transferRepository.findByCreatedAtBefore(endDate, pageable);
        } else {
            return transferRepository.findAll(pageable);
        }
    }

    // Task 10.3 - Settlement monitoring
    public Page<Transfer> getPendingSettlements(Pageable pageable) {
        return transferRepository.findByStatus(TransferStatus.PENDING.name(), pageable);
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

        // Check if settlement date has arrived
        if (transfer.getSettlementDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Transfer settlement date has not arrived");
        }

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
                .build();
        ledgerEntryRepository.save(receiverLedgerEntry);

        walletRepository.save(receiverWallet);

        // Update transfer status to SUCCESS
        transfer.setStatus(TransferStatus.SUCCESS);
        transferRepository.save(transfer);

        return transfer;
    }

    // Task 10.4 - Fraud review
    public Page<FraudAlert> getFlaggedTransactions(Pageable pageable) {
        Page<FraudAlert> openAlerts = fraudAlertRepository.findByStatus(FraudStatus.OPEN.name(), pageable);
        return openAlerts;
    }

    @Transactional
    public Transfer approveTransaction(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (transfer.getAdminReviewStatus() != AdminReviewStatus.PENDING) {
            throw new IllegalArgumentException("Transfer is not pending admin review");
        }

        // Update fraud alert if exists
        FraudAlert fraudAlert = fraudAlertRepository.findByTransferId(transferId).orElse(null);
        if (fraudAlert != null) {
            fraudAlert.setStatus(FraudStatus.APPROVED);
            fraudAlert.setReviewedAt(LocalDateTime.now());
            fraudAlertRepository.save(fraudAlert);
        }

        transfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
        transferRepository.save(transfer);

        return transfer;
    }

    @Transactional
    public Transfer refundTransaction(Long transferId) {
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
                .build();
        ledgerEntryRepository.save(refundLedgerEntry);

        walletRepository.save(senderWallet);

        // If receiver wallet was provided (settlement failure scenario), credit it too
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

        return transfer;
    }

    // Task 10.5 - Job execution history
    public List<ScheduledJobLog> getJobExecutionHistory(String jobName) {
        if (jobName != null) {
            return scheduledJobLogRepository.findByJobNameOrderByExecutionTimeDesc(jobName);
        }
        return scheduledJobLogRepository.findAll();
    }

    // Task 10.6 - Reconciliation
    public DailySummary getDailyReconciliation(LocalDate date) {
        return dailySummaryRepository.findBySummaryDate(date)
                .orElseThrow(() -> new IllegalArgumentException("Daily summary not found for date: " + date));
    }

    public List<DailySummary> getReconciliationReport(LocalDate startDate, LocalDate endDate) {
        return dailySummaryRepository.findBySummaryDateBetween(startDate, endDate);
    }

    // Response DTOs
    public static class DashboardData {
        private final BigDecimal volume30Day;
        private final long usersCount;
        private final long walletsCount;

        public DashboardData(BigDecimal volume30Day, long usersCount, long walletsCount) {
            this.volume30Day = volume30Day;
            this.usersCount = usersCount;
            this.walletsCount = walletsCount;
        }

        public BigDecimal getVolume30Day() { return volume30Day; }
        public long getUsersCount() { return usersCount; }
        public long getWalletsCount() { return walletsCount; }
    }
}
