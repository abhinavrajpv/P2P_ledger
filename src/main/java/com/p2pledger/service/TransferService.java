package com.p2pledger.service;

import com.p2pledger.annotation.RequirePin;
import com.p2pledger.model.*;
import com.p2pledger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final BigDecimal DAILY_LIMIT = BigDecimal.valueOf(50000);
    private static final BigDecimal ADMIN_REVIEW_THRESHOLD = BigDecimal.valueOf(8000);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @RequirePin(message = "Invalid transaction PIN", userIdIndex = 0, pinIndex = 4)
    public Transfer initiateTransfer(Long senderId, Long receiverId, BigDecimal amount, String requestId, String pin) {
        // Reject self-transfers
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        // Validate sender and receiver exist
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Validate wallet statuses
        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findByUserId(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Sender wallet is not active");
        }
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Receiver wallet is not active");
        }

        // Check for sufficient funds
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Check for duplicate requestId (idempotency)
        Transfer existingTransfer = transferRepository.findByRequestId(requestId).orElse(null);
        if (existingTransfer != null) {
            // Verify the existing transfer is for the same sender and has same details
            if (!existingTransfer.getSenderId().equals(senderId)) {
                throw new IllegalArgumentException("Request ID already used by another user");
            }
            if (!existingTransfer.getReceiverId().equals(receiverId) ||
                !existingTransfer.getAmount().equals(amount)) {
                throw new IllegalArgumentException("Request ID conflict: different transfer details");
            }
            // Idempotency: return original transfer
            return existingTransfer;
        }

        // Check for suspected duplicates (same receiver, same amount, within 2 minutes)
        if (isSuspectedDuplicate(senderId, receiverId, amount)) {
            throw new IllegalArgumentException("Suspected duplicate transfer detected");
        }

        // Check daily spending limit
        BigDecimal todaySpending = senderWallet.getTodaySpending();
        if (todaySpending.add(amount).compareTo(DAILY_LIMIT) > 0) {
            throw new IllegalArgumentException("Daily spending limit exceeded");
        }

        // Create transfer with PENDING status
        Transfer transfer = Transfer.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(amount)
                .requestId(requestId)
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .settlementDate(calculateSettlementDate())
                .fraudFlagged(false)
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .build();

        transfer = transferRepository.save(transfer);

        // Debit sender wallet atomically
        BigDecimal senderOldBalance = senderWallet.getBalance();
        BigDecimal senderNewBalance = senderOldBalance.subtract(amount);
        senderWallet.setBalance(senderNewBalance);
        senderWallet.setTodaySpending(todaySpending.add(amount));

        // Create sender DEBIT ledger entry
        LedgerEntry senderLedgerEntry = LedgerEntry.builder()
                .transferId(transfer.getId())
                .userId(senderId)
                .type(LedgerType.DEBIT)
                .amount(amount)
                .balanceBefore(senderOldBalance)
                .balanceAfter(senderNewBalance)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(senderLedgerEntry);

        walletRepository.save(senderWallet);

        // Create fraud alert if amount exceeds threshold
        boolean shouldFlag = amount.compareTo(ADMIN_REVIEW_THRESHOLD) > 0;
        if (shouldFlag) {
            createFraudAlert(transfer, "Transfer amount exceeds admin review threshold");
            transfer.setFraudFlagged(true);
            transfer.setAdminReviewStatus(AdminReviewStatus.PENDING);
            transferRepository.save(transfer);
        }

        // Create notification for sender
        createNotification(senderId, NotificationType.TRANSFER_INITIATED,
                "Transfer of ₹" + amount + " initiated to " + receiver.getName());

        // Update audit log
        createAuditLog(senderId, "TRANSFER_INITIATED", "Transfer", transfer.getId().toString(),
                "Transfer of " + amount + " to user " + receiverId);

        return transfer;
    }

    @Transactional
    public Transfer settleTransfer(Long transferId) {
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
            // Receiver wallet is not active - mark transfer as FAILED
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            createNotification(transfer.getSenderId(), NotificationType.FAILED,
                    "Transfer settlement failed: receiver wallet not active");
            return transfer;
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
                .createdAt(java.time.LocalDateTime.now())
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

        return transfer;
    }

    @Transactional
    public Transfer refundTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (transfer.getAdminReviewStatus() != AdminReviewStatus.PENDING) {
            throw new IllegalArgumentException("Transfer is not pending admin review");
        }

        // Check if already refunded (idempotency)
        if (transfer.getAdminReviewStatus() == AdminReviewStatus.REFUNDED) {
            return transfer;
        }

        Wallet senderWallet = walletRepository.findByUserId(transfer.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));

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
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(refundLedgerEntry);

        walletRepository.save(senderWallet);

        // Update transfer status
        transfer.setStatus(TransferStatus.DECLINED);
        transfer.setAdminReviewStatus(AdminReviewStatus.REFUNDED);
        transferRepository.save(transfer);

        // Update fraud alert
        FraudAlert fraudAlert = fraudAlertRepository.findByTransferId(transferId)
                .orElse(null);
        if (fraudAlert != null) {
            fraudAlert.setStatus(FraudStatus.RESOLVED);
            fraudAlert.setReviewedAt(LocalDateTime.now());
            fraudAlertRepository.save(fraudAlert);
        }

        // Create notifications
        createNotification(transfer.getSenderId(), NotificationType.REFUND,
                "Transfer of ₹" + amount + " refunded");
        createNotification(transfer.getReceiverId(), NotificationType.REFUND,
                "Transfer of ₹" + amount + " to you was refunded");

        // Update audit log
        createAuditLog(transfer.getSenderId(), "TRANSFER_REFUNDED", "Transfer", transferId.toString(),
                "Transfer " + transferId + " refunded by admin");

        return transfer;
    }

    public Page<TransferHistoryDto> getTransactionHistory(Long userId, Integer page, Integer size,
                                                           String type, String status, LocalDateTime startDate, LocalDateTime endDate) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        
        // Build query based on filters
        org.springframework.data.domain.Page<Transfer> transfers;
        
        if (startDate != null && endDate != null) {
            // Filter by date range
            transfers = transferRepository.findByUserId(userId, pageable).stream()
                    .filter(t -> t.getCreatedAt().isAfter(startDate.minusSeconds(1)) && 
                                 t.getCreatedAt().isBefore(endDate.plusSeconds(1)))
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                    ));
        } else if (startDate != null) {
            transfers = transferRepository.findByUserId(userId, pageable).stream()
                    .filter(t -> t.getCreatedAt().isAfter(startDate.minusSeconds(1)))
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                    ));
        } else if (endDate != null) {
            transfers = transferRepository.findByUserId(userId, pageable).stream()
                    .filter(t -> t.getCreatedAt().isBefore(endDate.plusSeconds(1)))
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                    ));
        } else {
            // No date filter
            transfers = transferRepository.findByUserIdWithFilters(userId, pageable);
        }
        
        // Get masking preference for this user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean maskingEnabled = user.isMaskingEnabled();
        
        // Map transfers to DTOs with masking applied and filters
        org.springframework.data.domain.Page<TransferHistoryDto> dtoPage = transfers.map(transfer -> {
            User sender = userRepository.findById(transfer.getSenderId())
                    .orElse(new User());
            User receiver = userRepository.findById(transfer.getReceiverId())
                    .orElse(new User());
            
            // Determine type from the perspective of userId
            String transferType = transfer.getSenderId().equals(userId) ? "DEBIT" : "CREDIT";
            
            // Apply type filter (client-side since it's perspective-based)
            if (type != null && !type.isEmpty() && !type.equalsIgnoreCase(transferType)) {
                return null; // Filter out
            }
            
            // Apply status filter
            if (status != null && !status.isEmpty() && !status.equalsIgnoreCase(transfer.getStatus().name())) {
                return null; // Filter out
            }

            return TransferHistoryDto.builder()
                    .id(transfer.getId())
                    .amount(transfer.getAmount())
                    .requestId(transfer.getRequestId())
                    .type(transferType)
                    .status(transfer.getStatus())
                    .createdAt(transfer.getCreatedAt())
                    .settlementDate(transfer.getSettlementDate())
                    .fraudFlagged(transfer.isFraudFlagged())
                    .adminReviewStatus(transfer.getAdminReviewStatus())
                    // Counterparty details - masked if user's masking preference is enabled
                    .senderName(maskingEnabled ? null : sender.getName())
                    .senderEmail(maskingEnabled ? null : sender.getEmail())
                    .senderPhone(maskingEnabled ? null : sender.getPhone())
                    .receiverName(maskingEnabled ? null : receiver.getName())
                    .receiverEmail(maskingEnabled ? null : receiver.getEmail())
                    .receiverPhone(maskingEnabled ? null : receiver.getPhone())
                    .daysUntilSettlement(calculateDaysUntilSettlement(transfer.getSettlementDate()))
                    .build();
        });
        
        // Remove null entries (filtered out)
        java.util.List<TransferHistoryDto> filteredList = dtoPage.getContent().stream()
                .filter(dto -> dto != null)
                .collect(java.util.stream.Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(filteredList, pageable, filteredList.size());
    }

    public Transfer getTransferById(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
    }
    
    public TransferDetailDto getTransferDetail(Long id, Long requestingUserId) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
        
        // Check authorization
        if (!transfer.getSenderId().equals(requestingUserId) && 
            !transfer.getReceiverId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Access denied");
        }
        
        // Get ledger entries for this transfer
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByTransferId(id);
        
        // Get user's masking preference
        User user = userRepository.findById(requestingUserId)
                .orElse(new User());
        boolean maskingEnabled = user.isMaskingEnabled();
        
        // Get sender and receiver details
        User sender = userRepository.findById(transfer.getSenderId()).orElse(new User());
        User receiver = userRepository.findById(transfer.getReceiverId()).orElse(new User());
        
        // Build DTO with conditional masking
        String senderName = maskingEnabled ? null : sender.getName();
        String senderEmail = maskingEnabled ? null : sender.getEmail();
        String senderPhone = maskingEnabled ? null : sender.getPhone();
        String receiverName = maskingEnabled ? null : receiver.getName();
        String receiverEmail = maskingEnabled ? null : receiver.getEmail();
        String receiverPhone = maskingEnabled ? null : receiver.getPhone();
        
        // Convert ledger entries to DTOs
        List<LedgerEntryDto> ledgerDtos = ledgerEntries.stream()
                .map(entry -> new LedgerEntryDto(
                    entry.getId(),
                    entry.getTransferId(),
                    entry.getUserId(),
                    entry.getType().name(),
                    entry.getAmount(),
                    entry.getBalanceBefore(),
                    entry.getBalanceAfter(),
                    entry.getCreatedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
        
        return TransferDetailDto.builder()
                .id(transfer.getId())
                .senderId(transfer.getSenderId())
                .receiverId(transfer.getReceiverId())
                .amount(transfer.getAmount())
                .requestId(transfer.getRequestId())
                .status(transfer.getStatus().name())
                .createdAt(transfer.getCreatedAt())
                .settlementDate(transfer.getSettlementDate())
                .fraudFlagged(transfer.isFraudFlagged())
                .adminReviewStatus(transfer.getAdminReviewStatus() != null ? transfer.getAdminReviewStatus().name() : null)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .senderPhone(senderPhone)
                .receiverName(receiverName)
                .receiverEmail(receiverEmail)
                .receiverPhone(receiverPhone)
                .ledgerEntries(ledgerDtos)
                .build();
    }

    private int calculateDaysUntilSettlement(LocalDateTime settlementDate) {
        if (settlementDate == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (settlementDate.isBefore(now)) return 0;
        return (int) java.time.Duration.between(now, settlementDate).toDays();
    }

    public TransferSummary getTransferSummary(Long userId) {
        List<Transfer> sentTransfers = transferRepository.findBySenderId(userId);
        List<Transfer> receivedTransfers = transferRepository.findByReceiverId(userId);

        BigDecimal totalSent = sentTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceived = receivedTransfers.stream()
                .filter(t -> t.getStatus() == TransferStatus.SUCCESS)
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long monthlyCount = sentTransfers.stream()
                .filter(t -> t.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1)))
                .count();

        return TransferSummary.builder()
                .totalSent(totalSent)
                .totalReceived(totalReceived)
                .monthlyCount(monthlyCount)
                .build();
    }

    private boolean isSuspectedDuplicate(Long senderId, Long receiverId, BigDecimal amount) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        return transferRepository.findBySenderIdAndReceiverIdAndAmountAndCreatedAtAfter(
                senderId, receiverId, amount, twoMinutesAgo).size() > 0;
    }

    private void createFraudAlert(Transfer transfer, String reason) {
        FraudAlert alert = FraudAlert.builder()
                .transferId(transfer.getId())
                .reason(reason)
                .status(FraudStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        fraudAlertRepository.save(alert);
    }

    private void createNotification(Long userId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
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
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    private LocalDateTime calculateSettlementDate() {
        // Calculate three business days (excluding weekends)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = now;
        int businessDays = 0;
        
        while (businessDays < 3) {
            result = result.plusDays(1);
            DayOfWeek day = result.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                businessDays++;
            }
        }
        
        return result;
    }

    public static class TransferHistoryDto {
        private final Long id;
        private final BigDecimal amount;
        private final String requestId;
        private final String type;  // "DEBIT" or "CREDIT" from caller's perspective
        private final TransferStatus status;
        private final LocalDateTime createdAt;
        private final LocalDateTime settlementDate;
        private final boolean fraudFlagged;
        private final AdminReviewStatus adminReviewStatus;
        // Counterparty details (masked if user's masking preference is enabled)
        private final String senderName;
        private final String senderEmail;
        private final String senderPhone;
        private final String receiverName;
        private final String receiverEmail;
        private final String receiverPhone;
        private final int daysUntilSettlement;

        public TransferHistoryDto() {
            this.id = null;
            this.amount = null;
            this.requestId = null;
            this.type = null;
            this.status = null;
            this.createdAt = null;
            this.settlementDate = null;
            this.fraudFlagged = false;
            this.adminReviewStatus = null;
            this.senderName = null;
            this.senderEmail = null;
            this.senderPhone = null;
            this.receiverName = null;
            this.receiverEmail = null;
            this.receiverPhone = null;
            this.daysUntilSettlement = 0;
        }

        public TransferHistoryDto(Long id, BigDecimal amount, String requestId, String type, TransferStatus status, LocalDateTime createdAt, LocalDateTime settlementDate,
                                   boolean fraudFlagged, AdminReviewStatus adminReviewStatus, String senderName, String senderEmail, String senderPhone,
                                   String receiverName, String receiverEmail, String receiverPhone, int daysUntilSettlement) {
            this.id = id;
            this.amount = amount;
            this.requestId = requestId;
            this.type = type;
            this.status = status;
            this.createdAt = createdAt;
            this.settlementDate = settlementDate;
            this.fraudFlagged = fraudFlagged;
            this.adminReviewStatus = adminReviewStatus;
            this.senderName = senderName;
            this.senderEmail = senderEmail;
            this.senderPhone = senderPhone;
            this.receiverName = receiverName;
            this.receiverEmail = receiverEmail;
            this.receiverPhone = receiverPhone;
            this.daysUntilSettlement = daysUntilSettlement;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Long getId() { return id; }
        public BigDecimal getAmount() { return amount; }
        public String getRequestId() { return requestId; }
        public String getType() { return type; }
        public TransferStatus getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getSettlementDate() { return settlementDate; }
        public boolean isFraudFlagged() { return fraudFlagged; }
        public AdminReviewStatus getAdminReviewStatus() { return adminReviewStatus; }
        public String getSenderName() { return senderName; }
        public String getSenderEmail() { return senderEmail; }
        public String getSenderPhone() { return senderPhone; }
        public String getReceiverName() { return receiverName; }
        public String getReceiverEmail() { return receiverEmail; }
        public String getReceiverPhone() { return receiverPhone; }
        public int getDaysUntilSettlement() { return daysUntilSettlement; }

        public static class Builder {
            private Long id;
            private BigDecimal amount;
            private String requestId;
            private String type;
            private TransferStatus status;
            private LocalDateTime createdAt;
            private LocalDateTime settlementDate;
            private boolean fraudFlagged;
            private AdminReviewStatus adminReviewStatus;
            private String senderName;
            private String senderEmail;
            private String senderPhone;
            private String receiverName;
            private String receiverEmail;
            private String receiverPhone;
            private int daysUntilSettlement;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
            public Builder requestId(String requestId) { this.requestId = requestId; return this; }
            public Builder type(String type) { this.type = type; return this; }
            public Builder status(TransferStatus status) { this.status = status; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder settlementDate(LocalDateTime settlementDate) { this.settlementDate = settlementDate; return this; }
            public Builder fraudFlagged(boolean fraudFlagged) { this.fraudFlagged = fraudFlagged; return this; }
            public Builder adminReviewStatus(AdminReviewStatus adminReviewStatus) { this.adminReviewStatus = adminReviewStatus; return this; }
            public Builder senderName(String senderName) { this.senderName = senderName; return this; }
            public Builder senderEmail(String senderEmail) { this.senderEmail = senderEmail; return this; }
            public Builder senderPhone(String senderPhone) { this.senderPhone = senderPhone; return this; }
            public Builder receiverName(String receiverName) { this.receiverName = receiverName; return this; }
            public Builder receiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; return this; }
            public Builder receiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; return this; }
            public Builder daysUntilSettlement(int daysUntilSettlement) { this.daysUntilSettlement = daysUntilSettlement; return this; }

            public TransferHistoryDto build() {
                return new TransferHistoryDto(id, amount, requestId, type, status, createdAt, settlementDate, fraudFlagged, adminReviewStatus,
                        senderName, senderEmail, senderPhone, receiverName, receiverEmail, receiverPhone, daysUntilSettlement);
            }
        }
    }

    // Response DTOs
    public static class TransferSummary {
        private final BigDecimal totalSent;
        private final BigDecimal totalReceived;
        private final long monthlyCount;

        public TransferSummary(BigDecimal totalSent, BigDecimal totalReceived, long monthlyCount) {
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
            this.monthlyCount = monthlyCount;
        }

        public BigDecimal getTotalSent() { return totalSent; }
        public BigDecimal getTotalReceived() { return totalReceived; }
        public long getMonthlyCount() { return monthlyCount; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BigDecimal totalSent;
            private BigDecimal totalReceived;
            private long monthlyCount;

            public Builder totalSent(BigDecimal totalSent) {
                this.totalSent = totalSent;
                return this;
            }

            public Builder totalReceived(BigDecimal totalReceived) {
                this.totalReceived = totalReceived;
                return this;
            }

            public Builder monthlyCount(long monthlyCount) {
                this.monthlyCount = monthlyCount;
                return this;
            }

            public TransferSummary build() {
                return new TransferSummary(totalSent, totalReceived, monthlyCount);
            }
        }
    }
    
    // Transfer Detail DTO with Ledger Entries
    public static class TransferDetailDto {
        private final Long id;
        private final Long senderId;
        private final Long receiverId;
        private final BigDecimal amount;
        private final String requestId;
        private final String status;
        private final LocalDateTime createdAt;
        private final LocalDateTime settlementDate;
        private final Boolean fraudFlagged;
        private final String adminReviewStatus;
        private final String senderName;
        private final String senderEmail;
        private final String senderPhone;
        private final String receiverName;
        private final String receiverEmail;
        private final String receiverPhone;
        private final List<LedgerEntryDto> ledgerEntries;
        
        public TransferDetailDto(Long id, Long senderId, Long receiverId, BigDecimal amount, String requestId,
                               String status, LocalDateTime createdAt, LocalDateTime settlementDate,
                               Boolean fraudFlagged, String adminReviewStatus, String senderName, String senderEmail,
                               String senderPhone, String receiverName, String receiverEmail, String receiverPhone,
                               List<LedgerEntryDto> ledgerEntries) {
            this.id = id;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.amount = amount;
            this.requestId = requestId;
            this.status = status;
            this.createdAt = createdAt;
            this.settlementDate = settlementDate;
            this.fraudFlagged = fraudFlagged;
            this.adminReviewStatus = adminReviewStatus;
            this.senderName = senderName;
            this.senderEmail = senderEmail;
            this.senderPhone = senderPhone;
            this.receiverName = receiverName;
            this.receiverEmail = receiverEmail;
            this.receiverPhone = receiverPhone;
            this.ledgerEntries = ledgerEntries;
        }
        
        public Long getId() { return id; }
        public Long getSenderId() { return senderId; }
        public Long getReceiverId() { return receiverId; }
        public BigDecimal getAmount() { return amount; }
        public String getRequestId() { return requestId; }
        public String getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getSettlementDate() { return settlementDate; }
        public Boolean getFraudFlagged() { return fraudFlagged; }
        public String getAdminReviewStatus() { return adminReviewStatus; }
        public String getSenderName() { return senderName; }
        public String getSenderEmail() { return senderEmail; }
        public String getSenderPhone() { return senderPhone; }
        public String getReceiverName() { return receiverName; }
        public String getReceiverEmail() { return receiverEmail; }
        public String getReceiverPhone() { return receiverPhone; }
        public List<LedgerEntryDto> getLedgerEntries() { return ledgerEntries; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Long id;
            private Long senderId;
            private Long receiverId;
            private BigDecimal amount;
            private String requestId;
            private String status;
            private LocalDateTime createdAt;
            private LocalDateTime settlementDate;
            private Boolean fraudFlagged;
            private String adminReviewStatus;
            private String senderName;
            private String senderEmail;
            private String senderPhone;
            private String receiverName;
            private String receiverEmail;
            private String receiverPhone;
            private List<LedgerEntryDto> ledgerEntries;
            
            public Builder id(Long id) { this.id = id; return this; }
            public Builder senderId(Long senderId) { this.senderId = senderId; return this; }
            public Builder receiverId(Long receiverId) { this.receiverId = receiverId; return this; }
            public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
            public Builder requestId(String requestId) { this.requestId = requestId; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder settlementDate(LocalDateTime settlementDate) { this.settlementDate = settlementDate; return this; }
            public Builder fraudFlagged(Boolean fraudFlagged) { this.fraudFlagged = fraudFlagged; return this; }
            public Builder adminReviewStatus(String adminReviewStatus) { this.adminReviewStatus = adminReviewStatus; return this; }
            public Builder senderName(String senderName) { this.senderName = senderName; return this; }
            public Builder senderEmail(String senderEmail) { this.senderEmail = senderEmail; return this; }
            public Builder senderPhone(String senderPhone) { this.senderPhone = senderPhone; return this; }
            public Builder receiverName(String receiverName) { this.receiverName = receiverName; return this; }
            public Builder receiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; return this; }
            public Builder receiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; return this; }
            public Builder ledgerEntries(List<LedgerEntryDto> ledgerEntries) { this.ledgerEntries = ledgerEntries; return this; }
            
            public TransferDetailDto build() {
                return new TransferDetailDto(id, senderId, receiverId, amount, requestId, status, createdAt,
                        settlementDate, fraudFlagged, adminReviewStatus, senderName, senderEmail, senderPhone,
                        receiverName, receiverEmail, receiverPhone, ledgerEntries);
            }
        }
    }
    
    // Ledger Entry DTO
    public static class LedgerEntryDto {
        private final Long id;
        private final Long transferId;
        private final Long userId;
        private final String type;
        private final BigDecimal amount;
        private final BigDecimal balanceBefore;
        private final BigDecimal balanceAfter;
        private final LocalDateTime createdAt;
        
        public LedgerEntryDto(Long id, Long transferId, Long userId, String type, BigDecimal amount,
                            BigDecimal balanceBefore, BigDecimal balanceAfter, LocalDateTime createdAt) {
            this.id = id;
            this.transferId = transferId;
            this.userId = userId;
            this.type = type;
            this.amount = amount;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.createdAt = createdAt;
        }
        
        public Long getId() { return id; }
        public Long getTransferId() { return transferId; }
        public Long getUserId() { return userId; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getBalanceBefore() { return balanceBefore; }
        public BigDecimal getBalanceAfter() { return balanceAfter; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
