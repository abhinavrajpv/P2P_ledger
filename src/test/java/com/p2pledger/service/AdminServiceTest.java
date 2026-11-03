package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

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
    @Mock
    private LoginAttemptsRepository loginAttemptsRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private Wallet testWallet;
    private Transfer testTransfer;
    private FraudAlert testFraudAlert;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setRole(Role.USER);
        testUser.setMaskingEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(5));

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUserId(1L);
        testWallet.setBalance(BigDecimal.valueOf(1000));
        testWallet.setStatus(WalletStatus.ACTIVE);
        testWallet.setTodaySpending(BigDecimal.ZERO);
        testWallet.setDailyLimitResetDate(LocalDate.now());

        testTransfer = Transfer.builder()
                .id(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(500))
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .settlementDate(LocalDateTime.now().plusHours(23))
                .adminReviewStatus(AdminReviewStatus.PENDING)
                .fraudFlagged(true)
                .build();

        testFraudAlert = FraudAlert.builder()
                .id(1L)
                .transferId(1L)
                .reason("Large transaction")
                .status(FraudStatus.OPEN)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void testGetDashboardData_Success() {
        // Arrange
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        
        List<Transfer> recentTransfers = Arrays.asList(
                createTransfer(1L, BigDecimal.valueOf(100), TransferStatus.SUCCESS, LocalDateTime.now().minusDays(5)),
                createTransfer(2L, BigDecimal.valueOf(200), TransferStatus.SUCCESS, LocalDateTime.now().minusDays(10)),
                createTransfer(3L, BigDecimal.valueOf(300), TransferStatus.PENDING, LocalDateTime.now())
        );
        
        List<Transfer> todayTransfers = Arrays.asList(
                createTransfer(4L, BigDecimal.valueOf(50), TransferStatus.SUCCESS, LocalDateTime.now()),
                createTransfer(5L, BigDecimal.valueOf(75), TransferStatus.FAILED, LocalDateTime.now())
        );
        
        List<Transfer> pendingTransfers = Arrays.asList(
                createTransfer(6L, BigDecimal.valueOf(300), TransferStatus.PENDING, LocalDateTime.now())
        );

        when(transferRepository.findAll()).thenReturn(recentTransfers);
        when(transferRepository.findByCreatedAtBetween(todayStart, todayEnd, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(todayTransfers));
        when(transferRepository.findByStatus(TransferStatus.PENDING)).thenReturn(pendingTransfers);
        when(userRepository.count()).thenReturn(10L);
        when(walletRepository.count()).thenReturn(10L);
        when(fraudAlertRepository.countByStatus(FraudStatus.OPEN)).thenReturn(2L);

        // Act
        AdminService.DashboardData result = adminService.getDashboardData();

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(300), result.getVolume30Day());
        assertEquals(10L, result.getUsersCount());
        assertEquals(10L, result.getWalletsCount());
        assertEquals(1L, result.getPendingSettlements());
        assertEquals(2L, result.getFlaggedTransactions());
        assertEquals(BigDecimal.valueOf(50), result.getTodayVolume());
        assertEquals(2L, result.getTodayCount());
        assertEquals(1L, result.getTodaySuccessCount());
        assertEquals(1L, result.getTodayFailedCount());
        assertEquals(BigDecimal.valueOf(300), result.getPendingSettlementAmount());
    }

    @Test
    void testGetAllUsersWithWallet_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        // Act
        Page<AdminService.UserWithWallet> result = adminService.getAllUsersWithWallet(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        AdminService.UserWithWallet userWithWallet = result.getContent().get(0);
        assertEquals(testUser.getName(), userWithWallet.getName());
        assertEquals(testWallet.getBalance(), userWithWallet.getWalletBalance());
        assertEquals(testWallet.getStatus(), userWithWallet.getWalletStatus());
    }

    @Test
    void testGetAllUsersWithWallet_NoWallet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act
        Page<AdminService.UserWithWallet> result = adminService.getAllUsersWithWallet(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        AdminService.UserWithWallet userWithWallet = result.getContent().get(0);
        assertEquals(BigDecimal.ZERO, userWithWallet.getWalletBalance());
        assertNull(userWithWallet.getWalletStatus());
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // Act
        Page<User> result = adminService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTransactionsWithFilters_InvalidStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, "INVALID_STATUS", null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transferRepository, never()).findAll(pageable);
    }

    @Test
    void testGetTransactionsWithFilters_BySenderId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findBySenderId(1L, pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, null, 1L, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transferRepository).findBySenderId(1L, pageable);
    }

    @Test
    void testGetTransactionsWithFilters_BySenderIdAndStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findBySenderIdAndStatus(1L, TransferStatus.PENDING, pageable))
                .thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, "PENDING", 1L, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transferRepository).findBySenderIdAndStatus(1L, TransferStatus.PENDING, pageable);
    }

    @Test
    void testGetTransactionsWithFilters_ByReceiverId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findByReceiverId(2L, pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, null, null, 2L, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transferRepository).findByReceiverId(2L, pageable);
    }

    @Test
    void testGetTransactionsWithFilters_ByReceiverIdAndStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findByReceiverIdAndStatus(2L, TransferStatus.SUCCESS, pageable))
                .thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, "SUCCESS", null, 2L, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTransactionsWithFilters_ByStatusOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findByStatus(TransferStatus.PENDING, pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, "PENDING", null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transferRepository).findByStatus(TransferStatus.PENDING, pageable);
    }

    @Test
    void testGetTransactionsWithFilters_ByDateRange() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findByCreatedAtBetween(start, end, pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, null, null, null, start, end, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTransactionsWithFilters_WithAmountRange() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Transfer transfer1 = createTransfer(1L, BigDecimal.valueOf(100), TransferStatus.SUCCESS, LocalDateTime.now());
        Transfer transfer2 = createTransfer(2L, BigDecimal.valueOf(500), TransferStatus.SUCCESS, LocalDateTime.now());
        Transfer transfer3 = createTransfer(3L, BigDecimal.valueOf(1000), TransferStatus.SUCCESS, LocalDateTime.now());
        
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(transfer1, transfer2, transfer3));
        when(transferRepository.findAll(pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getTransactionsWithFilters(
                pageable, null, null, null, null, null, BigDecimal.valueOf(200), BigDecimal.valueOf(800));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(BigDecimal.valueOf(500), result.getContent().get(0).getAmount());
    }

    @Test
    void testGetPendingSettlements() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transfer> transfers = new PageImpl<>(Arrays.asList(testTransfer));
        when(transferRepository.findByStatus(TransferStatus.PENDING, pageable)).thenReturn(transfers);

        // Act
        Page<Transfer> result = adminService.getPendingSettlements(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetSettlementSummary() {
        // Arrange
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime tomorrowEnd = LocalDate.now().plusDays(1).atTime(23, 59, 59);
        
        Transfer overdueTransfer = createTransfer(1L, BigDecimal.valueOf(100), TransferStatus.PENDING, 
                LocalDateTime.now().minusDays(2));
        overdueTransfer.setSettlementDate(todayStart.minusHours(1));
        
        Transfer dueTodayTransfer = createTransfer(2L, BigDecimal.valueOf(200), TransferStatus.PENDING, 
                LocalDateTime.now());
        dueTodayTransfer.setSettlementDate(todayStart.plusHours(12));
        
        Transfer dueTomorrowTransfer = createTransfer(3L, BigDecimal.valueOf(300), TransferStatus.PENDING, 
                LocalDateTime.now());
        dueTomorrowTransfer.setSettlementDate(todayEnd.plusHours(12));
        
        List<Transfer> pendingTransfers = Arrays.asList(overdueTransfer, dueTodayTransfer, dueTomorrowTransfer);
        when(transferRepository.findByStatus(TransferStatus.PENDING)).thenReturn(pendingTransfers);

        // Act
        AdminService.SettlementSummary result = adminService.getSettlementSummary();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getDueToday());
        assertEquals(1L, result.getDueTomorrow());
        assertEquals(1L, result.getOverdue());
        assertEquals(3L, result.getTotalPending());
    }

    @Test
    void testProcessSettlement_Success() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
        
        Wallet senderWallet = new Wallet();
        senderWallet.setUserId(1L);
        senderWallet.setBalance(BigDecimal.valueOf(1000));
        
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUserId(2L);
        receiverWallet.setBalance(BigDecimal.valueOf(500));
        receiverWallet.setStatus(WalletStatus.ACTIVE);
        
        User sender = new User();
        sender.setId(1L);
        sender.setName("Sender");
        
        User receiver = new User();
        receiver.setId(2L);
        receiver.setName("Receiver");

        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // Act
        Transfer result = adminService.processSettlement(1L, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(TransferStatus.SUCCESS, result.getStatus());
        verify(walletRepository).save(receiverWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testProcessSettlement_TransferNotFound() {
        // Arrange
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.processSettlement(999L, 100L);
        });
    }

    @Test
    void testProcessSettlement_NotPendingStatus() {
        // Arrange
        testTransfer.setStatus(TransferStatus.SUCCESS);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.processSettlement(1L, 100L);
        });
    }

    @Test
    void testProcessSettlement_PendingAdminReview() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.PENDING);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.processSettlement(1L, 100L);
        });
    }

    @Test
    void testProcessSettlement_InactiveReceiverWallet() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
        
        Wallet senderWallet = new Wallet();
        senderWallet.setUserId(1L);
        senderWallet.setBalance(BigDecimal.valueOf(1000));
        senderWallet.setTodaySpending(BigDecimal.valueOf(500));
        
        Wallet receiverWallet = new Wallet();
        receiverWallet.setUserId(2L);
        receiverWallet.setStatus(WalletStatus.FROZEN);

        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transfer result = adminService.processSettlement(1L, 100L);

        // Assert
        assertEquals(TransferStatus.DECLINED, result.getStatus());
        assertEquals(AdminReviewStatus.REFUNDED, result.getAdminReviewStatus());
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void testGetFlaggedTransactions() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<FraudAlert> fraudAlerts = new PageImpl<>(Arrays.asList(testFraudAlert));
        when(fraudAlertRepository.findByStatus(FraudStatus.OPEN, pageable)).thenReturn(fraudAlerts);

        // Act
        Page<FraudAlert> result = adminService.getFlaggedTransactions(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testApproveTransaction_Success() {
        // Arrange
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testFraudAlert));
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testFraudAlert);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // Act
        Transfer result = adminService.approveTransaction(1L, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(AdminReviewStatus.APPROVED, result.getAdminReviewStatus());
        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testApproveTransaction_FraudAlertNotFound() {
        // Arrange
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.approveTransaction(999L, 100L);
        });
    }

    @Test
    void testApproveTransaction_NotPendingReview() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testFraudAlert));
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.approveTransaction(1L, 100L);
        });
    }

    @Test
    void testRefundTransaction_Success() {
        // Arrange
        Wallet senderWallet = new Wallet();
        senderWallet.setUserId(1L);
        senderWallet.setBalance(BigDecimal.valueOf(500));
        senderWallet.setTodaySpending(BigDecimal.valueOf(500));

        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testFraudAlert));
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudAlertRepository.findByTransferId(1L)).thenReturn(Optional.of(testFraudAlert));

        // Act
        Transfer result = adminService.refundTransaction(1L, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(TransferStatus.DECLINED, result.getStatus());
        assertEquals(AdminReviewStatus.REFUNDED, result.getAdminReviewStatus());
        verify(walletRepository).save(senderWallet);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(notificationRepository).save(any(Notification.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testRefundTransaction_AlreadyRefunded() {
        // Arrange
        testTransfer.setAdminReviewStatus(AdminReviewStatus.REFUNDED);
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testFraudAlert));
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));

        // Act
        Transfer result = adminService.refundTransaction(1L, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(AdminReviewStatus.REFUNDED, result.getAdminReviewStatus());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testGetJobExecutionHistory_WithJobName() {
        // Arrange
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("test_job").build()
        );
        when(scheduledJobLogRepository.findByJobNameOrderByExecutionTimeDesc("test_job"))
                .thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = adminService.getJobExecutionHistory("test_job");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetJobExecutionHistory_WithoutJobName() {
        // Arrange
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("job1").build(),
                ScheduledJobLog.builder().jobName("job2").build()
        );
        when(scheduledJobLogRepository.findAll()).thenReturn(logs);

        // Act
        List<ScheduledJobLog> result = adminService.getJobExecutionHistory(null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetDailyReconciliation_Success() {
        // Arrange
        LocalDate date = LocalDate.now();
        DailySummary summary = DailySummary.builder()
                .summaryDate(date)
                .totalTransfersInitiated(10L)
                .totalAmountSettled(BigDecimal.valueOf(1000))
                .build();
        when(dailySummaryRepository.findBySummaryDate(date)).thenReturn(Optional.of(summary));

        // Act
        DailySummary result = adminService.getDailyReconciliation(date);

        // Assert
        assertNotNull(result);
        assertEquals(date, result.getSummaryDate());
        assertEquals(10L, result.getTotalTransfersInitiated());
    }

    @Test
    void testGetDailyReconciliation_NotFound() {
        // Arrange
        LocalDate date = LocalDate.now();
        when(dailySummaryRepository.findBySummaryDate(date)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.getDailyReconciliation(date);
        });
    }

    @Test
    void testGetReconciliationReport() {
        // Arrange
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        List<DailySummary> summaries = Arrays.asList(
                DailySummary.builder().summaryDate(start).build(),
                DailySummary.builder().summaryDate(end).build()
        );
        when(dailySummaryRepository.findBySummaryDateBetween(start, end)).thenReturn(summaries);

        // Act
        List<DailySummary> result = adminService.getReconciliationReport(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetDailyVolumeChart() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(29);
        LocalDate endDate = LocalDate.now();
        
        DailySummary summary = DailySummary.builder()
                .summaryDate(startDate)
                .totalAmountSettled(BigDecimal.valueOf(1000))
                .build();
        
        when(dailySummaryRepository.findBySummaryDateBetween(any(), any()))
                .thenReturn(Arrays.asList(summary));
        when(transferRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        // Act
        List<AdminService.DailyVolumeData> result = adminService.getDailyVolumeChart();

        // Assert
        assertNotNull(result);
        assertEquals(30, result.size());
    }

    @Test
    void testGetUserDetail() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(transferRepository.findAll()).thenReturn(Arrays.asList(testTransfer));

        // Act
        AdminService.UserDetail result = adminService.getUserDetail(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testWallet.getBalance(), result.getWalletBalance());
    }

    @Test
    void testGetUserDetail_NotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.getUserDetail(999L);
        });
    }

    @Test
    void testFreezeUserWallet_Success() {
        // Arrange
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        // Act
        adminService.freezeUserWallet(1L, 100L);

        // Assert
        assertEquals(WalletStatus.FROZEN, testWallet.getStatus());
        verify(walletRepository).save(testWallet);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testFreezeUserWallet_AlreadyFrozen() {
        // Arrange
        testWallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.freezeUserWallet(1L, 100L);
        });
    }

    @Test
    void testFreezeUserWallet_WalletNotFound() {
        // Arrange
        when(walletRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.freezeUserWallet(999L, 100L);
        });
    }

    @Test
    void testUnfreezeUserWallet_Success() {
        // Arrange
        testWallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        // Act
        adminService.unfreezeUserWallet(1L, 100L);

        // Assert
        assertEquals(WalletStatus.ACTIVE, testWallet.getStatus());
        verify(walletRepository).save(testWallet);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testUnfreezeUserWallet_NotFrozen() {
        // Arrange
        testWallet.setStatus(WalletStatus.ACTIVE);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.unfreezeUserWallet(1L, 100L);
        });
    }

    @Test
    void testGetLoginAttempts_AllFilters() {
        // Arrange
        LoginAttempts attempt1 = LoginAttempts.builder()
                .userId(1L)
                .identifier("test@example.com")
                .successful(true)
                .attemptedAt(LocalDateTime.now())
                .build();
        LoginAttempts attempt2 = LoginAttempts.builder()
                .userId(2L)
                .identifier("other@example.com")
                .successful(false)
                .attemptedAt(LocalDateTime.now())
                .build();
        
        when(loginAttemptsRepository.findAll()).thenReturn(Arrays.asList(attempt1, attempt2));

        // Act
        List<LoginAttempts> result = adminService.getLoginAttempts(1L, "test@example.com", true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void testGetLoginAttempts_NoFilters() {
        // Arrange
        List<LoginAttempts> attempts = Arrays.asList(
                LoginAttempts.builder().attemptedAt(LocalDateTime.now()).build(),
                LoginAttempts.builder().attemptedAt(LocalDateTime.now().minusHours(1)).build()
        );
        when(loginAttemptsRepository.findAll()).thenReturn(attempts);

        // Act
        List<LoginAttempts> result = adminService.getLoginAttempts(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetFailedLoginAttempts() {
        // Arrange
        List<LoginAttempts> failedAttempts = Arrays.asList(
                LoginAttempts.builder().successful(false).build()
        );
        when(loginAttemptsRepository.findBySuccessfulFalseOrderByAttemptedAtDesc())
                .thenReturn(failedAttempts);

        // Act
        List<LoginAttempts> result = adminService.getFailedLoginAttempts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Helper method
    private Transfer createTransfer(Long id, BigDecimal amount, TransferStatus status, LocalDateTime createdAt) {
        Transfer transfer = Transfer.builder()
                .id(id)
                .senderId(1L)
                .receiverId(2L)
                .amount(amount)
                .status(status)
                .createdAt(createdAt)
                .settlementDate(createdAt.plusDays(2))
                .adminReviewStatus(AdminReviewStatus.NOT_REQUIRED)
                .build();
        return transfer;
    }
}
