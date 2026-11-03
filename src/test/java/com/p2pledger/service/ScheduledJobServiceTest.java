package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private FraudAlertRepository fraudAlertRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ScheduledJobLogRepository scheduledJobLogRepository;
    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private SettlementBatchRepository settlementBatchRepository;

    @InjectMocks
    private ScheduledJobService scheduledJobService;

    private Transfer testTransfer;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
    testUser = new User();
testUser.setId(1L);
testUser.setName("Test User");
testUser.setRole(Role.USER);
testUser.setCreatedAt(LocalDateTime.now().minusDays(1));

adminUser = new User();
adminUser.setId(100L);
adminUser.setName("Admin User");
adminUser.setRole(Role.ADMIN);
adminUser.setCreatedAt(LocalDateTime.now());

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUserId(1L);
        senderWallet.setBalance(BigDecimal.valueOf(500));
        senderWallet.setStatus(WalletStatus.ACTIVE);
        senderWallet.setTodaySpending(BigDecimal.valueOf(500));
        senderWallet.setDailyLimitResetDate(LocalDate.now());

        receiverWallet = new Wallet();
        receiverWallet.setId(2L);
        receiverWallet.setUserId(2L);
        receiverWallet.setBalance(BigDecimal.valueOf(1000));
        receiverWallet.setStatus(WalletStatus.ACTIVE);
        receiverWallet.setTodaySpending(BigDecimal.ZERO);
        receiverWallet.setDailyLimitResetDate(LocalDate.now());

        testTransfer = Transfer.builder()
                .id(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(500))
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(2))
                .settlementDate(LocalDateTime.now().minusHours(1))
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .fraudFlagged(false)
                .build();
    }

    @Test
    void testSettleEligibleTransfers_Success() {
        // Arrange
        when(transferRepository.findAll()).thenReturn(Arrays.asList(testTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduledJobService.settleEligibleTransfers();

        // Assert
        verify(transferRepository).save(testTransfer);
        verify(walletRepository).save(receiverWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
        verify(settlementBatchRepository, times(2)).save(any(SettlementBatch.class));
    }

    @Test
    void testSettleEligibleTransfers_WithPendingAdminReview() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.PENDING);
        FraudAlert fraudAlert = FraudAlert.builder()
                .id(1L)
                .transferId(1L)
                .status(FraudStatus.OPEN)
                .build();

        when(transferRepository.findAll()).thenReturn(Arrays.asList(testTransfer));
        when(fraudAlertRepository.findByTransferId(1L)).thenReturn(Optional.of(fraudAlert));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.save(any(FraudAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduledJobService.settleEligibleTransfers();

        // Assert
        verify(fraudAlertRepository).save(fraudAlert);
        assertEquals(FraudStatus.APPROVED, fraudAlert.getStatus());
        verify(transferRepository, times(2)).save(testTransfer);
        assertEquals(AdminReviewStatus.APPROVED, testTransfer.getAdminReviewStatus());
    }

    @Test
    void testSettleEligibleTransfers_InactiveReceiverWallet() {
        // Arrange
        receiverWallet.setStatus(WalletStatus.FROZEN);
        when(transferRepository.findAll()).thenReturn(Arrays.asList(testTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduledJobService.settleEligibleTransfers();

        // Assert
        verify(transferRepository, atLeastOnce()).save(any(Transfer.class));
        verify(walletRepository).save(senderWallet); // Refund
        verify(settlementBatchRepository, times(2)).save(any(SettlementBatch.class));
    }

    @Test
    void testSettleEligibleTransfers_NoEligibleTransfers() {
        // Arrange
        Transfer futureTransfer = Transfer.builder()
                .status(TransferStatus.PENDING)
                .settlementDate(LocalDateTime.now().plusDays(1))
                .build();
        when(transferRepository.findAll()).thenReturn(Arrays.asList(futureTransfer));
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduledJobService.settleEligibleTransfers();

        // Assert
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testSettleEligibleTransfers_WithFailure() {
        // Arrange
        Transfer failingTransfer = Transfer.builder()
                .id(2L)
                .status(TransferStatus.PENDING)
                .settlementDate(LocalDateTime.now().minusHours(1))
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(100))
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .build();

        when(transferRepository.findAll()).thenReturn(Arrays.asList(testTransfer, failingTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L))
                .thenReturn(Optional.of(receiverWallet))
                .thenThrow(new RuntimeException("Wallet error"));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(settlementBatchRepository.save(any(SettlementBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduledJobService.settleEligibleTransfers();

        // Assert
        verify(settlementBatchRepository, times(2)).save(any(SettlementBatch.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testResetDailyLimits_Success() {
        // Arrange
        List<Wallet> wallets = Arrays.asList(senderWallet, receiverWallet);
        when(walletRepository.findAll()).thenReturn(wallets);

        // Act
        scheduledJobService.resetDailyLimits();

        // Assert
        verify(walletRepository, times(2)).save(any(Wallet.class));
        assertEquals(BigDecimal.ZERO, senderWallet.getTodaySpending());
        assertEquals(LocalDate.now(), senderWallet.getDailyLimitResetDate());
        assertEquals(BigDecimal.ZERO, receiverWallet.getTodaySpending());
        assertEquals(LocalDate.now(), receiverWallet.getDailyLimitResetDate());
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testResetDailyLimits_WithException() {
        // Arrange
        when(walletRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act
        scheduledJobService.resetDailyLimits();

        // Assert
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testUnlockLockedAccounts_Success() {
        // Arrange
        User lockedUser = new User();
        lockedUser.setId(10L);
        lockedUser.setLockedUntil(LocalDateTime.now().minusHours(1));
        lockedUser.setFailedLoginCount(5);

        when(userRepository.findAll()).thenReturn(Arrays.asList(lockedUser, testUser));

        // Act
        scheduledJobService.unlockLockedAccounts();

        // Assert
        verify(userRepository).save(lockedUser);
        assertNull(lockedUser.getLockedUntil());
        assertEquals(0, lockedUser.getFailedLoginCount());
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testUnlockLockedAccounts_NoLockedAccounts() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        scheduledJobService.unlockLockedAccounts();

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testUnlockLockedAccounts_StillLocked() {
        // Arrange
        User stillLockedUser = new User();
        stillLockedUser.setId(10L);
        stillLockedUser.setLockedUntil(LocalDateTime.now().plusHours(1));

        when(userRepository.findAll()).thenReturn(Arrays.asList(stillLockedUser));

        // Act
        scheduledJobService.unlockLockedAccounts();

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testGenerateDailySummaryAndReconciliation_NewSummary() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        Transfer todayTransfer = Transfer.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(100))
                .status(TransferStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        List<Transfer> todayTransfers = Arrays.asList(todayTransfer);
        
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transferId(1L)
                .type(LedgerType.DEBIT)
                .amount(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transferId(1L)
                .type(LedgerType.CREDIT)
                .amount(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .build();

        when(dailySummaryRepository.findBySummaryDate(today)).thenReturn(Optional.empty());
        when(transferRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(todayTransfers));
        when(fraudAlertRepository.findAll()).thenReturn(Arrays.asList());
        when(userRepository.findAll()).thenReturn(Arrays.asList());
        when(ledgerEntryRepository.findByCreatedAtBetween(startOfDay, endOfDay))
                .thenReturn(Arrays.asList(debitEntry, creditEntry));

        // Act
        scheduledJobService.generateDailySummaryAndReconciliation();

        // Assert
        verify(dailySummaryRepository).save(any(DailySummary.class));
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testGenerateDailySummaryAndReconciliation_UpdateExisting() {
        // Arrange
        LocalDate today = LocalDate.now();
        DailySummary existingSummary = DailySummary.builder()
                .id(1L)
                .summaryDate(today)
                .totalTransfersInitiated(5L)
                .totalAmountInitiated(BigDecimal.valueOf(500))
                .build();

        Transfer todayTransfer = Transfer.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(100))
                .status(TransferStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(dailySummaryRepository.findBySummaryDate(today)).thenReturn(Optional.of(existingSummary));
        when(transferRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Arrays.asList(todayTransfer)));
        when(fraudAlertRepository.findAll()).thenReturn(Arrays.asList());
        when(userRepository.findAll()).thenReturn(Arrays.asList());
        when(ledgerEntryRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(Arrays.asList());

        // Act
        scheduledJobService.generateDailySummaryAndReconciliation();

        // Assert
        verify(dailySummaryRepository).save(existingSummary);
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

    @Test
    void testGenerateDailySummaryAndReconciliation_WithMismatch() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        Transfer todayTransfer = Transfer.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(100))
                .status(TransferStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .transferId(1L)
                .type(LedgerType.DEBIT)
                .amount(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transferId(1L)
                .type(LedgerType.CREDIT)
                .amount(BigDecimal.valueOf(90)) // Mismatch
                .createdAt(LocalDateTime.now())
                .build();

        when(dailySummaryRepository.findBySummaryDate(today)).thenReturn(Optional.empty());
        when(transferRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Arrays.asList(todayTransfer)));
        when(fraudAlertRepository.findAll()).thenReturn(Arrays.asList());
        when(userRepository.findAll()).thenReturn(Arrays.asList(adminUser));
        when(ledgerEntryRepository.findByCreatedAtBetween(startOfDay, endOfDay))
                .thenReturn(Arrays.asList(debitEntry, creditEntry));

        // Act
        scheduledJobService.generateDailySummaryAndReconciliation();

        // Assert
        verify(dailySummaryRepository).save(any(DailySummary.class));
        verify(notificationRepository).save(any(Notification.class)); // Mismatch notification
        verify(scheduledJobLogRepository).save(any(ScheduledJobLog.class));
    }

 @Test
void testGenerateDailySummaryAndReconciliation_WithException() {
    // Arrange
    LocalDate today = LocalDate.now();

    when(dailySummaryRepository.findBySummaryDate(today))
            .thenReturn(Optional.empty());

    when(transferRepository.findByCreatedAtBetween(any(), any(), any()))
            .thenThrow(new RuntimeException("Database error"));

    // Act & Assert
    RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> scheduledJobService.generateDailySummaryAndReconciliation()
    );

    assertEquals("Database error", exception.getMessage());

    // Verify that execution stopped before the job log was saved
    verify(scheduledJobLogRepository, never()).save(any(ScheduledJobLog.class));
}
    @Test
    void testGetJobExecutionHistory_WithJobName() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("test_job").build()
        );
        when(scheduledJobLogRepository.findTop20ByJobNameOrderByExecutionTimeDesc("test_job", pageable))
                .thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = scheduledJobService.getJobExecutionHistory("test_job");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetJobExecutionHistory_WithoutJobName() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("job1").build(),
                ScheduledJobLog.builder().jobName("job2").build()
        );
        when(scheduledJobLogRepository.findTop20ByOrderByExecutionTimeDesc(pageable))
                .thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = scheduledJobService.getJobExecutionHistory(null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetJobExecutionHistory_EmptyJobName() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("job1").build()
        );
        when(scheduledJobLogRepository.findTop20ByOrderByExecutionTimeDesc(pageable))
                .thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = scheduledJobService.getJobExecutionHistory("   ");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllJobExecutionHistory() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("job1").build()
        );
        when(scheduledJobLogRepository.findTop20ByOrderByExecutionTimeDesc(pageable))
                .thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = scheduledJobService.getAllJobExecutionHistory();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
