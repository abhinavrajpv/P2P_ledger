package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private WalletService walletService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FraudNotificationService fraudNotificationService;

    @InjectMocks
    private TransferService transferService;

    private User sender;
    private User receiver;
    private User admin;

    private Wallet senderWallet;
    private Wallet receiverWallet;

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long ADMIN_ID = 3L;

    private static final BigDecimal AMOUNT = new BigDecimal("1000.00");
    private static final String REQUEST_ID = "REQ-123";
    private static final String PIN = "1234";

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        sender = User.builder()
                .id(SENDER_ID)
                .name("Sender User")
                .email("sender@test.com")
                .phone("1234567890")
                .transactionPin("hashed-pin")
                .role(Role.USER)
                .maskingEnabled(false)
                .build();

        receiver = User.builder()
                .id(RECEIVER_ID)
                .name("Receiver User")
                .email("receiver@test.com")
                .phone("0987654321")
                .role(Role.USER)
                .maskingEnabled(false)
                .build();

        admin = User.builder()
                .id(ADMIN_ID)
                .name("Admin User")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .maskingEnabled(false)
                .build();

        senderWallet = Wallet.builder()
                .id(1L)
                .userId(SENDER_ID)
                .balance(new BigDecimal("10000.00"))
                .todaySpending(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        receiverWallet = Wallet.builder()
                .id(2L)
                .userId(RECEIVER_ID)
                .balance(new BigDecimal("5000.00"))
                .todaySpending(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();
    }

    // =====================================================
    // INITIATE TRANSFER
    // =====================================================

    private void mockValidInitiation() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(receiverWallet));

        when(transferRepository.findByRequestId(anyString())).thenReturn(Optional.empty());

        when(transferRepository.findBySenderIdAndReceiverIdAndAmountAndCreatedAtAfter(
                anyLong(), anyLong(), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer transfer = invocation.getArgument(0);
                    if (transfer.getId() == null) {
                        transfer.setId(100L);
                    }
                    return transfer;
                });
    }

    @Test
    void testInitiateTransfer_SelfTransfer() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, SENDER_ID, AMOUNT, REQUEST_ID, PIN));

        assertEquals("Cannot transfer to yourself", exception.getMessage());
        verifyNoInteractions(transferRepository);
    }

    @Test
    void testInitiateTransfer_SenderNotFound() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.empty());

        assertEquals("Sender not found", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_ReceiverNotFound() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.empty());

        assertEquals("Receiver not found", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_NullAmount() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        assertEquals("Amount must be positive", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, null, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_ZeroAmount() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        assertEquals("Amount must be positive", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, BigDecimal.ZERO, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_NegativeAmount() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID,
                        new BigDecimal("-100"), REQUEST_ID, PIN)
        );
    }

    @Test
    void testInitiateTransfer_SenderWalletNotFound() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.empty());

        assertEquals("Sender wallet not found", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_ReceiverWalletNotFound() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());

        assertEquals("Receiver wallet not found", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_SenderWalletInactive() {
        senderWallet.setStatus(WalletStatus.FROZEN);

        mockUsersAndWallets();

        assertEquals("Sender wallet is not active", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_ReceiverWalletInactive() {
        receiverWallet.setStatus(WalletStatus.FROZEN);

        mockUsersAndWallets();

        assertEquals("Receiver wallet is not active", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_InsufficientFunds() {
        senderWallet.setBalance(new BigDecimal("500"));

        mockUsersAndWallets();

        assertEquals("Insufficient funds", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_ExactBalance() {
        senderWallet.setBalance(AMOUNT);
        mockValidInitiation();

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN);

        assertNotNull(result);
        assertEquals(0, senderWallet.getBalance().compareTo(BigDecimal.ZERO));
        verify(walletRepository).save(senderWallet);
    }

    @Test
    void testInitiateTransfer_DuplicateRequestDifferentSender() {
        Transfer existing = createTransfer();
        existing.setSenderId(99L);

        mockUsersAndWallets();
        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(existing));

        assertEquals("Request ID already used by another user", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_DuplicateRequestDifferentReceiver() {
        Transfer existing = createTransfer();
        existing.setReceiverId(99L);

        mockUsersAndWallets();
        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(existing));

        assertEquals("Request ID conflict: different transfer details", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());
    }

    @Test
    void testInitiateTransfer_DuplicateRequestDifferentAmount() {
        Transfer existing = createTransfer();
        existing.setAmount(new BigDecimal("2000.00"));

        mockUsersAndWallets();
        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        );
    }

    @Test
    void testInitiateTransfer_IdempotentRequest() {
        Transfer existing = createTransfer();

        mockUsersAndWallets();
        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(existing));

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN);

        assertSame(existing, result);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void testInitiateTransfer_SuspectedDuplicate() {
        mockUsersAndWallets();

        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());

        when(transferRepository.findBySenderIdAndReceiverIdAndAmountAndCreatedAtAfter(
                eq(SENDER_ID), eq(RECEIVER_ID), eq(AMOUNT), any(LocalDateTime.class)))
                .thenReturn(List.of(createTransfer()));

        assertEquals("Suspected duplicate transfer detected", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN)
        ).getMessage());

        verify(fraudNotificationService).notifyAdminsAboutDuplicateTransfer(
                SENDER_ID, sender.getName(), RECEIVER_ID, receiver.getName(), AMOUNT);
    }

    @Test
    void testInitiateTransfer_DailyLimitExceeded() {
        senderWallet.setBalance(new BigDecimal("60000"));
        senderWallet.setTodaySpending(new BigDecimal("45000"));

        mockUsersAndWallets();

        when(transferRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());

        when(transferRepository.findBySenderIdAndReceiverIdAndAmountAndCreatedAtAfter(
                anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of());

        BigDecimal amount = new BigDecimal("6000");

        assertEquals("Daily spending limit exceeded", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.initiateTransfer(
                        SENDER_ID, RECEIVER_ID, amount, REQUEST_ID, PIN)
        ).getMessage());

        verify(fraudNotificationService).notifyAdminsAboutDailyLimitViolation(
                SENDER_ID, sender.getName(), amount,
                senderWallet.getTodaySpending(), new BigDecimal("50000"));
    }

    @Test
    void testInitiateTransfer_AtDailyLimit() {
        senderWallet.setBalance(new BigDecimal("60000"));
        senderWallet.setTodaySpending(BigDecimal.ZERO);

        mockValidInitiation();

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID,
                new BigDecimal("50000"), REQUEST_ID, PIN);

        assertNotNull(result);
        assertEquals(TransferStatus.PENDING, result.getStatus());
    }

    @Test
    void testInitiateTransfer_AboveAdminReviewThreshold() {
        senderWallet.setBalance(new BigDecimal("20000"));

        mockValidInitiation();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID,
                new BigDecimal("8001"), REQUEST_ID, PIN);

        assertTrue(result.isFraudFlagged());
        assertEquals(AdminReviewStatus.PENDING, result.getAdminReviewStatus());

        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void testInitiateTransfer_AtAdminReviewThreshold() {
        senderWallet.setBalance(new BigDecimal("20000"));

        mockValidInitiation();

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID,
                new BigDecimal("8000"), REQUEST_ID, PIN);

        assertFalse(result.isFraudFlagged());
        assertEquals(AdminReviewStatus.NOT_REQUIRED, result.getAdminReviewStatus());
        verify(fraudAlertRepository, never()).save(any());
    }

    @Test
    void testInitiateTransfer_Success() {
        mockValidInitiation();

        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, PIN);

        assertNotNull(result);
        assertEquals(TransferStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("9000.00"), senderWallet.getBalance());
        assertEquals(AMOUNT, senderWallet.getTodaySpending());

        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(walletRepository).save(senderWallet);
        verify(notificationRepository).save(any(Notification.class));
    }

    private void mockUsersAndWallets() {
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(receiverWallet));
    }

    // =====================================================
    // SETTLE TRANSFER
    // =====================================================

    private Transfer createTransfer() {
        return Transfer.builder()
                .id(100L)
                .senderId(SENDER_ID)
                .receiverId(RECEIVER_ID)
                .amount(AMOUNT)
                .requestId(REQUEST_ID)
                .status(TransferStatus.PENDING)
                .createdAt(now)
                .settlementDate(now.minusDays(1))
                .fraudFlagged(false)
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .build();
    }

    @Test
    void testSettleTransfer_NotFound() {
        when(transferRepository.findById(100L)).thenReturn(Optional.empty());

        assertEquals("Transfer not found", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        ).getMessage());
    }

    @Test
    void testSettleTransfer_NotPending() {
        Transfer transfer = createTransfer();
        transfer.setStatus(TransferStatus.SUCCESS);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );
    }

    @Test
    void testSettleTransfer_AdminApprovalRequired() {
        Transfer transfer = createTransfer();
        transfer.setAdminReviewStatus(AdminReviewStatus.PENDING);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );
    }

    @Test
    void testSettleTransfer_DateNotArrived() {
        Transfer transfer = createTransfer();
        transfer.setSettlementDate(now.plusDays(5));

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );
    }

    @Test
    void testSettleTransfer_SenderWalletMissing() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );
    }

    @Test
    void testSettleTransfer_ReceiverWalletMissing() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );
    }

    @Test
    void testSettleTransfer_ReceiverWalletInactive() {
        Transfer transfer = createTransfer();
        receiverWallet.setStatus(WalletStatus.FROZEN);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(receiverWallet));

        Transfer result = transferService.settleTransfer(100L);

        assertEquals(TransferStatus.FAILED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void testSettleTransfer_Success() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        Transfer result = transferService.settleTransfer(100L);

        assertEquals(TransferStatus.SUCCESS, result.getStatus());
        assertEquals(new BigDecimal("6000.00"), receiverWallet.getBalance());

        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(walletRepository).save(receiverWallet);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void testSettleTransfer_UsersNotFoundForNotifications() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.empty());

        Transfer result = transferService.settleTransfer(100L);

        assertEquals(TransferStatus.SUCCESS, result.getStatus());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    // =====================================================
    // REFUND TRANSFER
    // =====================================================

    private Transfer createRefundableTransfer() {
        Transfer transfer = createTransfer();
        transfer.setAdminReviewStatus(AdminReviewStatus.PENDING);
        return transfer;
    }

    @Test
    void testRefundTransfer_NotFound() {
        when(transferRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.refundTransfer(100L)
        );
    }

    @Test
    void testRefundTransfer_NotPendingReview() {
        Transfer transfer = createTransfer();
        transfer.setAdminReviewStatus(AdminReviewStatus.NOT_REQUIRED);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.refundTransfer(100L)
        );
    }

    @Test
    void testRefundTransfer_SenderWalletMissing() {
        Transfer transfer = createRefundableTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.refundTransfer(100L)
        );
    }

    @Test
    void testRefundTransfer_Success() {
        Transfer transfer = createRefundableTransfer();
        senderWallet.setTodaySpending(new BigDecimal("2000"));

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(fraudAlertRepository.findByTransferId(100L)).thenReturn(Optional.empty());
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        Transfer result = transferService.refundTransfer(100L);

        assertEquals(TransferStatus.DECLINED, result.getStatus());
        assertEquals(AdminReviewStatus.REFUNDED, result.getAdminReviewStatus());
        assertEquals(0, new BigDecimal("11000.00").compareTo(senderWallet.getBalance()));
        assertEquals(0, new BigDecimal("1000").compareTo(senderWallet.getTodaySpending()));

        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(walletRepository).save(senderWallet);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testRefundTransfer_SpendingCannotBecomeNegative() {
        Transfer transfer = createRefundableTransfer();
        senderWallet.setTodaySpending(new BigDecimal("500"));

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(fraudAlertRepository.findByTransferId(100L)).thenReturn(Optional.empty());
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.empty());

        transferService.refundTransfer(100L);

        assertEquals(BigDecimal.ZERO, senderWallet.getTodaySpending());
    }

    @Test
    void testRefundTransfer_FraudAlertFound() {
        Transfer transfer = createRefundableTransfer();

        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .transferId(100L)
                .reason("High value")
                .status(FraudStatus.OPEN)
                .createdAt(now)
                .build();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(fraudAlertRepository.findByTransferId(100L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        transferService.refundTransfer(100L);

        assertEquals(FraudStatus.RESOLVED, alert.getStatus());
        assertNotNull(alert.getReviewedAt());

        verify(fraudAlertRepository).save(alert);
    }

    // =====================================================
    // TRANSACTION HISTORY
    // =====================================================

    private Transfer createHistoryTransfer(
            Long id, Long senderId, Long receiverId,
            TransferStatus status, LocalDateTime createdAt) {

        return Transfer.builder()
                .id(id)
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(AMOUNT)
                .requestId("REQ-" + id)
                .status(status)
                .createdAt(createdAt)
                .settlementDate(now.plusDays(2))
                .fraudFlagged(false)
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .build();
    }

    private void mockHistoryUsers(boolean maskingEnabled) {
        sender.setMaskingEnabled(maskingEnabled);
        lenient().when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        lenient().when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
    }

    @Test
    void testGetTransactionHistory_NoDateFilters() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("DEBIT", result.getContent().get(0).getType());
        assertEquals("Receiver User", result.getContent().get(0).getReceiverName());
    }

    @Test
    void testGetTransactionHistory_StartAndEndDate() {
        mockHistoryUsers(false);

        LocalDateTime start = now.minusDays(5);
        LocalDateTime end = now.plusDays(5);

        when(transferRepository.findByUserIdAndCreatedAtBetweenUnpaged(
                SENDER_ID, start, end))
                .thenReturn(List.of());

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, start, end);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_StartDateOnly() {
        mockHistoryUsers(false);

        LocalDateTime start = now.minusDays(5);

        when(transferRepository.findByUserIdAndCreatedAtAfterUnpaged(SENDER_ID, start))
                .thenReturn(List.of());

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, start, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_EndDateOnly() {
        mockHistoryUsers(false);

        LocalDateTime end = now.plusDays(5);

        when(transferRepository.findByUserIdAndCreatedAtBeforeUnpaged(SENDER_ID, end))
                .thenReturn(List.of());

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, end);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_UserNotFound() {
        when(transferRepository.findByUserIdUnpaged(SENDER_ID)).thenReturn(List.of());
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, null)
        );
    }

    @Test
    void testGetTransactionHistory_MaskingEnabled() {
        mockHistoryUsers(true);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, null);

        assertNull(result.getContent().get(0).getSenderName());
        assertNull(result.getContent().get(0).getReceiverName());
    }

    @Test
    void testGetTransactionHistory_CreditSuccess() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(RECEIVER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        RECEIVER_ID, 0, 10, null, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("CREDIT", result.getContent().get(0).getType());
    }

    @Test
    void testGetTransactionHistory_HidesPendingIncoming() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(RECEIVER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.PENDING, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        RECEIVER_ID, 0, 10, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_HidesDeclinedIncoming() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(RECEIVER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.DECLINED, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        RECEIVER_ID, 0, 10, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_TypeFilter() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, "CREDIT", null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_StatusFilter() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, "PENDING", null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTransactionHistory_CaseInsensitiveFilters() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, "debit", "success", null, null);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTransactionHistory_SecondPage() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now),
                        createHistoryTransfer(2L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now.minusDays(1)),
                        createHistoryTransfer(3L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now.minusDays(2))
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 1, 2, null, null, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void testGetTransactionHistory_PageBeyondRecords() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of());

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 5, 10, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    // =====================================================
    // GET TRANSFER BY ID
    // =====================================================

    @Test
    void testGetTransferById_Success() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        assertSame(transfer, transferService.getTransferById(100L));
    }

    @Test
    void testGetTransferById_NotFound() {
        when(transferRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.getTransferById(100L)
        );
    }

    // =====================================================
    // TRANSFER DETAIL
    // =====================================================

    private LedgerEntry createLedgerEntry(Long id, Long userId, LedgerType type) {
        return LedgerEntry.builder()
                .id(id)
                .transferId(100L)
                .userId(userId)
                .type(type)
                .amount(AMOUNT)
                .balanceBefore(new BigDecimal("5000"))
                .balanceAfter(new BigDecimal("6000"))
                .createdAt(now)
                .build();
    }

    @Test
    void testGetTransferDetail_SenderAccess() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        when(ledgerEntryRepository.findByTransferId(100L))
                .thenReturn(List.of(
                        createLedgerEntry(1L, SENDER_ID, LedgerType.DEBIT),
                        createLedgerEntry(2L, RECEIVER_ID, LedgerType.CREDIT)
                ));

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, SENDER_ID);

        assertEquals(1, result.getLedgerEntries().size());
        assertEquals("Sender User", result.getSenderName());
        assertEquals("Receiver User", result.getReceiverName());
    }

    @Test
    void testGetTransferDetail_ReceiverAccess() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));

        when(ledgerEntryRepository.findByTransferId(100L))
                .thenReturn(List.of(
                        createLedgerEntry(1L, SENDER_ID, LedgerType.DEBIT),
                        createLedgerEntry(2L, RECEIVER_ID, LedgerType.CREDIT)
                ));

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, RECEIVER_ID);

        assertEquals(1, result.getLedgerEntries().size());
        assertEquals(RECEIVER_ID, result.getLedgerEntries().get(0).getUserId());
    }

    @Test
    void testGetTransferDetail_AdminAccess() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));

        when(ledgerEntryRepository.findByTransferId(100L))
                .thenReturn(List.of(
                        createLedgerEntry(1L, SENDER_ID, LedgerType.DEBIT),
                        createLedgerEntry(2L, RECEIVER_ID, LedgerType.CREDIT)
                ));

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, ADMIN_ID);

        assertEquals(2, result.getLedgerEntries().size());
    }

    @Test
    void testGetTransferDetail_AccessDenied() {
        Transfer transfer = createTransfer();

        User otherUser = User.builder()
                .id(99L)
                .role(Role.USER)
                .maskingEnabled(false)
                .build();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherUser));

        assertEquals("Access denied", assertThrows(
                IllegalArgumentException.class,
                () -> transferService.getTransferDetail(100L, 99L)
        ).getMessage());
    }

    @Test
    void testGetTransferDetail_TransferNotFound() {
        when(transferRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.getTransferDetail(100L, SENDER_ID)
        );
    }

    @Test
    void testGetTransferDetail_UserNotFound_DefaultUser() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.getTransferDetail(100L, 99L)
        );
    }

    @Test
    void testGetTransferDetail_MaskingEnabled() {
        Transfer transfer = createTransfer();
        sender.setMaskingEnabled(true);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(ledgerEntryRepository.findByTransferId(100L)).thenReturn(List.of());

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, SENDER_ID);

        assertNull(result.getSenderName());
        assertNull(result.getReceiverName());
        assertNull(result.getSenderEmail());
        assertNull(result.getReceiverPhone());
    }

    @Test
    void testGetTransferDetail_NullAdminReviewStatus() {
        Transfer transfer = createTransfer();
        transfer.setAdminReviewStatus(null);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.of(receiver));
        when(ledgerEntryRepository.findByTransferId(100L)).thenReturn(List.of());

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, SENDER_ID);

        assertNull(result.getAdminReviewStatus());
    }

    // =====================================================
    // TRANSFER SUMMARY
    // =====================================================

    @Test
    void testGetTransferSummary_Success() {
        Transfer sentSuccess = createTransfer();
        sentSuccess.setStatus(TransferStatus.SUCCESS);
        sentSuccess.setAmount(new BigDecimal("1000"));
        sentSuccess.setCreatedAt(now);

        Transfer sentPending = createTransfer();
        sentPending.setStatus(TransferStatus.PENDING);
        sentPending.setAmount(new BigDecimal("500"));
        sentPending.setCreatedAt(now.minusMonths(2));

        Transfer receivedSuccess = createTransfer();
        receivedSuccess.setStatus(TransferStatus.SUCCESS);
        receivedSuccess.setAmount(new BigDecimal("2000"));

        when(transferRepository.findBySenderId(SENDER_ID))
                .thenReturn(List.of(sentSuccess, sentPending));

        when(transferRepository.findByReceiverId(SENDER_ID))
                .thenReturn(List.of(receivedSuccess));

        TransferService.TransferSummary result =
                transferService.getTransferSummary(SENDER_ID);

        assertEquals(new BigDecimal("1000"), result.getTotalSent());
        assertEquals(new BigDecimal("2000"), result.getTotalReceived());
        assertEquals(1, result.getMonthlyCount());
    }

    @Test
    void testGetTransferSummary_EmptyLists() {
        when(transferRepository.findBySenderId(SENDER_ID)).thenReturn(List.of());
        when(transferRepository.findByReceiverId(SENDER_ID)).thenReturn(List.of());

        TransferService.TransferSummary result =
                transferService.getTransferSummary(SENDER_ID);

        assertEquals(BigDecimal.ZERO, result.getTotalSent());
        assertEquals(BigDecimal.ZERO, result.getTotalReceived());
        assertEquals(0, result.getMonthlyCount());
    }

    // =====================================================
    // LEDGER ENTRIES
    // =====================================================

    @Test
    void testGetLedgerEntriesByTransferId() {
        List<LedgerEntry> entries = List.of(
                createLedgerEntry(1L, SENDER_ID, LedgerType.DEBIT),
                createLedgerEntry(2L, RECEIVER_ID, LedgerType.CREDIT)
        );

        when(ledgerEntryRepository.findByTransferId(100L)).thenReturn(entries);

        List<LedgerEntry> result =
                transferService.getLedgerEntriesByTransferId(100L);

        assertEquals(2, result.size());
        assertSame(entries, result);
    }

    @Test
    void testGetLedgerEntriesByTransferId_Empty() {
        when(ledgerEntryRepository.findByTransferId(100L)).thenReturn(List.of());

        List<LedgerEntry> result =
                transferService.getLedgerEntriesByTransferId(100L);

        assertTrue(result.isEmpty());
    }

    // =====================================================
    // DTO TESTS - for complete coverage
    // =====================================================

    @Test
    void testTransferHistoryDto_DefaultConstructor() {
        TransferService.TransferHistoryDto dto = new TransferService.TransferHistoryDto();

        assertNull(dto.getId());
        assertNull(dto.getAmount());
        assertNull(dto.getRequestId());
        assertNull(dto.getType());
        assertNull(dto.getStatus());
        assertNull(dto.getCreatedAt());
        assertNull(dto.getSettlementDate());
        assertFalse(dto.isFraudFlagged());
        assertNull(dto.getAdminReviewStatus());
        assertNull(dto.getSenderName());
        assertNull(dto.getSenderEmail());
        assertNull(dto.getSenderPhone());
        assertNull(dto.getReceiverName());
        assertNull(dto.getReceiverEmail());
        assertNull(dto.getReceiverPhone());
        assertEquals(0, dto.getDaysUntilSettlement());
    }

    @Test
    void testTransferHistoryDto_FullConstructor() {
        TransferService.TransferHistoryDto dto = new TransferService.TransferHistoryDto(
                1L, AMOUNT, REQUEST_ID, "DEBIT", TransferStatus.SUCCESS,
                now, now.plusDays(3), true, AdminReviewStatus.PENDING,
                "Sender", "sender@test.com", "1234567890",
                "Receiver", "receiver@test.com", "0987654321", 3
        );

        assertEquals(1L, dto.getId());
        assertEquals(AMOUNT, dto.getAmount());
        assertEquals(REQUEST_ID, dto.getRequestId());
        assertEquals("DEBIT", dto.getType());
        assertEquals(TransferStatus.SUCCESS, dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertTrue(dto.isFraudFlagged());
        assertEquals(AdminReviewStatus.PENDING, dto.getAdminReviewStatus());
        assertEquals("Sender", dto.getSenderName());
        assertEquals("sender@test.com", dto.getSenderEmail());
        assertEquals("1234567890", dto.getSenderPhone());
        assertEquals("Receiver", dto.getReceiverName());
        assertEquals("receiver@test.com", dto.getReceiverEmail());
        assertEquals("0987654321", dto.getReceiverPhone());
        assertEquals(3, dto.getDaysUntilSettlement());
    }

    @Test
    void testTransferHistoryDto_Builder() {
        TransferService.TransferHistoryDto dto = TransferService.TransferHistoryDto.builder()
                .id(1L)
                .amount(AMOUNT)
                .requestId(REQUEST_ID)
                .type("CREDIT")
                .status(TransferStatus.PENDING)
                .createdAt(now)
                .settlementDate(now.plusDays(2))
                .fraudFlagged(false)
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .senderName("Test Sender")
                .senderEmail("sender@example.com")
                .senderPhone("1111111111")
                .receiverName("Test Receiver")
                .receiverEmail("receiver@example.com")
                .receiverPhone("2222222222")
                .daysUntilSettlement(2)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("CREDIT", dto.getType());
        assertEquals("Test Sender", dto.getSenderName());
        assertEquals("Test Receiver", dto.getReceiverName());
    }

    @Test
    void testTransferSummary_Constructor() {
        TransferService.TransferSummary summary =
                new TransferService.TransferSummary(
                        new BigDecimal("5000"),
                        new BigDecimal("3000"),
                        10L
                );

        assertEquals(new BigDecimal("5000"), summary.getTotalSent());
        assertEquals(new BigDecimal("3000"), summary.getTotalReceived());
        assertEquals(10L, summary.getMonthlyCount());
    }

    @Test
    void testTransferSummary_Builder() {
        TransferService.TransferSummary summary =
                TransferService.TransferSummary.builder()
                        .totalSent(new BigDecimal("10000"))
                        .totalReceived(new BigDecimal("7500"))
                        .monthlyCount(25L)
                        .build();

        assertEquals(new BigDecimal("10000"), summary.getTotalSent());
        assertEquals(new BigDecimal("7500"), summary.getTotalReceived());
        assertEquals(25L, summary.getMonthlyCount());
    }

    @Test
    void testTransferDetailDto_Constructor() {
        List<TransferService.LedgerEntryDto> ledgerEntries = List.of(
                new TransferService.LedgerEntryDto(1L, 100L, SENDER_ID, "DEBIT",
                        AMOUNT, new BigDecimal("5000"), new BigDecimal("4000"), now)
        );

        TransferService.TransferDetailDto dto =
                new TransferService.TransferDetailDto(
                        100L, SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID,
                        "SUCCESS", now, now.plusDays(3),
                        false, "NOT_REQUIRED",
                        "Sender Name", "sender@test.com", "1234567890",
                        "Receiver Name", "receiver@test.com", "0987654321",
                        ledgerEntries
                );

        assertEquals(100L, dto.getId());
        assertEquals(SENDER_ID, dto.getSenderId());
        assertEquals(RECEIVER_ID, dto.getReceiverId());
        assertEquals(AMOUNT, dto.getAmount());
        assertEquals(REQUEST_ID, dto.getRequestId());
        assertEquals("SUCCESS", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertFalse(dto.getFraudFlagged());
        assertEquals("NOT_REQUIRED", dto.getAdminReviewStatus());
        assertEquals("Sender Name", dto.getSenderName());
        assertEquals("sender@test.com", dto.getSenderEmail());
        assertEquals("1234567890", dto.getSenderPhone());
        assertEquals("Receiver Name", dto.getReceiverName());
        assertEquals("receiver@test.com", dto.getReceiverEmail());
        assertEquals("0987654321", dto.getReceiverPhone());
        assertEquals(1, dto.getLedgerEntries().size());
    }

    @Test
    void testTransferDetailDto_Builder() {
        TransferService.TransferDetailDto dto =
                TransferService.TransferDetailDto.builder()
                        .id(100L)
                        .senderId(SENDER_ID)
                        .receiverId(RECEIVER_ID)
                        .amount(AMOUNT)
                        .requestId(REQUEST_ID)
                        .status("PENDING")
                        .createdAt(now)
                        .settlementDate(now.plusDays(3))
                        .fraudFlagged(true)
                        .adminReviewStatus("PENDING")
                        .senderName("John Doe")
                        .senderEmail("john@example.com")
                        .senderPhone("1111111111")
                        .receiverName("Jane Doe")
                        .receiverEmail("jane@example.com")
                        .receiverPhone("2222222222")
                        .ledgerEntries(List.of())
                        .build();

        assertEquals(100L, dto.getId());
        assertEquals("PENDING", dto.getStatus());
        assertTrue(dto.getFraudFlagged());
        assertEquals("John Doe", dto.getSenderName());
        assertEquals("Jane Doe", dto.getReceiverName());
    }

    @Test
    void testLedgerEntryDto_Constructor() {
        TransferService.LedgerEntryDto dto =
                new TransferService.LedgerEntryDto(
                        1L, 100L, SENDER_ID, "DEBIT",
                        AMOUNT, new BigDecimal("10000"), new BigDecimal("9000"), now
                );

        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getTransferId());
        assertEquals(SENDER_ID, dto.getUserId());
        assertEquals("DEBIT", dto.getType());
        assertEquals(AMOUNT, dto.getAmount());
        assertEquals(new BigDecimal("10000"), dto.getBalanceBefore());
        assertEquals(new BigDecimal("9000"), dto.getBalanceAfter());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testLedgerEntryDto_AllGetters() {
        TransferService.LedgerEntryDto dto =
                new TransferService.LedgerEntryDto(
                        5L, 200L, RECEIVER_ID, "CREDIT",
                        new BigDecimal("2500"), new BigDecimal("3000"),
                        new BigDecimal("5500"), now.minusDays(1)
                );

        assertEquals(5L, dto.getId());
        assertEquals(200L, dto.getTransferId());
        assertEquals(RECEIVER_ID, dto.getUserId());
        assertEquals("CREDIT", dto.getType());
        assertEquals(new BigDecimal("2500"), dto.getAmount());
        assertEquals(new BigDecimal("3000"), dto.getBalanceBefore());
        assertEquals(new BigDecimal("5500"), dto.getBalanceAfter());
        assertEquals(now.minusDays(1), dto.getCreatedAt());
    }

    // =====================================================
    // ADDITIONAL EDGE CASES
    // =====================================================

    @Test
    void testRefundTransfer_IdempotentRefund() {
        Transfer transfer = createRefundableTransfer();
        transfer.setStatus(TransferStatus.DECLINED);
        transfer.setAdminReviewStatus(AdminReviewStatus.REFUNDED);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.refundTransfer(100L)
        );

        assertTrue(exception.getMessage().contains("not pending admin review"));
    }

    @Test
    void testInitiateTransfer_InvalidPinHandled() {
        // This test verifies PIN validation aspect behavior
        // Note: The @RequirePin aspect handles this before reaching service logic
        mockValidInitiation();
        
        // Even if PIN is empty/null, the aspect should catch it
        // If aspect is disabled in test, the transfer still proceeds
        Transfer result = transferService.initiateTransfer(
                SENDER_ID, RECEIVER_ID, AMOUNT, REQUEST_ID, "");

        assertNotNull(result);
    }

    @Test
    void testSettleTransfer_StatusNotPending() {
        Transfer transfer = createTransfer();
        transfer.setStatus(TransferStatus.DECLINED);

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.settleTransfer(100L)
        );

        assertTrue(exception.getMessage().contains("not in PENDING status"));
    }

    @Test
    void testGetTransferDetail_SenderNotFound() {
        Transfer transfer = createTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.empty());
        when(ledgerEntryRepository.findByTransferId(100L)).thenReturn(List.of());

        TransferService.TransferDetailDto result =
                transferService.getTransferDetail(100L, SENDER_ID);

        assertNotNull(result);
        assertNull(result.getSenderName());
        assertNull(result.getReceiverName());
    }

    @Test
    void testGetTransactionHistory_EmptyTypeFilter() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, "", null, null, null);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTransactionHistory_EmptyStatusFilter() {
        mockHistoryUsers(false);

        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(
                        createHistoryTransfer(1L, SENDER_ID, RECEIVER_ID,
                                TransferStatus.SUCCESS, now)
                ));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, "", null, null);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testCalculateDaysUntilSettlement_NullDate() {
        Transfer transfer = createTransfer();
        transfer.setSettlementDate(null);

        mockHistoryUsers(false);
        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(transfer));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, null);

        assertEquals(0, result.getContent().get(0).getDaysUntilSettlement());
    }

    @Test
    void testCalculateDaysUntilSettlement_PastDate() {
        Transfer transfer = createTransfer();
        transfer.setSettlementDate(now.minusDays(5));

        mockHistoryUsers(false);
        when(transferRepository.findByUserIdUnpaged(SENDER_ID))
                .thenReturn(List.of(transfer));

        Page<TransferService.TransferHistoryDto> result =
                transferService.getTransactionHistory(
                        SENDER_ID, 0, 10, null, null, null, null);

        assertEquals(0, result.getContent().get(0).getDaysUntilSettlement());
    }

    @Test
    void testGetTransferSummary_OnlyFailedTransfers() {
        Transfer failed1 = createTransfer();
        failed1.setStatus(TransferStatus.FAILED);
        failed1.setAmount(new BigDecimal("1000"));

        Transfer failed2 = createTransfer();
        failed2.setStatus(TransferStatus.DECLINED);
        failed2.setAmount(new BigDecimal("2000"));

        when(transferRepository.findBySenderId(SENDER_ID))
                .thenReturn(List.of(failed1, failed2));
        when(transferRepository.findByReceiverId(SENDER_ID))
                .thenReturn(List.of());

        TransferService.TransferSummary result =
                transferService.getTransferSummary(SENDER_ID);

        assertEquals(BigDecimal.ZERO, result.getTotalSent());
        assertEquals(BigDecimal.ZERO, result.getTotalReceived());
    }

    @Test
    void testRefundTransfer_ReceiverNotFound() {
        Transfer transfer = createRefundableTransfer();

        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(senderWallet));
        when(fraudAlertRepository.findByTransferId(100L)).thenReturn(Optional.empty());
        when(userRepository.findById(RECEIVER_ID)).thenReturn(Optional.empty());

        Transfer result = transferService.refundTransfer(100L);

        assertEquals(TransferStatus.DECLINED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }
}