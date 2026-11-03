package com.p2pledger.service;

import com.p2pledger.model.*;
import com.p2pledger.repository.LedgerEntryRepository;
import com.p2pledger.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit tests for WalletService
 * 
 * MUTATION TESTING GUIDE:
 * 
 * 1. Line ~35: if (amount.compareTo(BigDecimal.ZERO) <= 0) - Change <= to <
 *    - Test: testDeposit_ZeroAmount_ThrowsException() will FAIL
 * 
 * 2. Line ~44: BigDecimal newBalance = oldBalance.add(amount) - Change add to subtract
 *    - Test: testDeposit_VerifyBalanceIncreased() will FAIL
 * 
 * 3. Line ~78: if (currentStatus == status) - Change == to !=
 *    - Test: testUpdateStatus_SameStatus_NoChange() will FAIL
 * 
 * 4. Line ~83: if (status == WalletStatus.ACTIVE && currentStatus == WalletStatus.FROZEN)
 *    - Change && to ||
 *    - Test: testUpdateStatus_AdminUnfreeze_Success() will FAIL
 * 
 * 5. Line ~95: if (status == WalletStatus.FROZEN) - Remove this condition
 *    - Test: testUpdateStatus_AdminFreeze_Success() will FAIL
 * 
 * 6. LedgerType.CREDIT in deposit - Change to DEBIT
 *    - Test: testDeposit_VerifyLedgerEntry() will FAIL
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;
    private static final Long USER_ID = 1L;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000.00");
    private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("5000.00");
    private static final String REFERENCE = "DEP-123";

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .id(1L)
                .userId(USER_ID)
                .balance(INITIAL_BALANCE)
                .todaySpending(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();
    }

    // ========== GET BALANCE TESTS ==========

    @Test
    void testGetBalance_WalletExists_ReturnsBalance() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletService.WalletBalanceResponse response = walletService.getBalance(USER_ID);

        assertNotNull(response);
        assertEquals(INITIAL_BALANCE, response.getBalance());
        assertEquals(BigDecimal.ZERO, response.getTodaySpending());
        assertEquals(WalletStatus.ACTIVE, response.getStatus());
        assertEquals(new BigDecimal("50000"), response.getDailyLimit());
    }

    @Test
    void testGetBalance_WalletNotFound_ThrowsException() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.getBalance(USER_ID)
        );

        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    // ========== DEPOSIT TESTS - MUTATION CRITICAL ==========

    @Test
    void testDeposit_ZeroAmount_ThrowsException() {
        // Mutation: Change <= to < on amount comparison
        // This test will FAIL if mutation is applied
        
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.deposit(USER_ID, BigDecimal.ZERO, REFERENCE)
        );

        assertEquals("Amount must be positive", exception.getMessage());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testDeposit_NegativeAmount_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.deposit(USER_ID, new BigDecimal("-100"), REFERENCE)
        );

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    void testDeposit_NullAmount_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.deposit(USER_ID, null, REFERENCE)
        );

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    void testDeposit_ValidAmount_Success() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletService.WalletDepositResponse response = walletService.deposit(USER_ID, DEPOSIT_AMOUNT, REFERENCE);

        assertNotNull(response);
        assertEquals(DEPOSIT_AMOUNT, response.getAmount());
        assertEquals(INITIAL_BALANCE.add(DEPOSIT_AMOUNT), response.getNewBalance());
        assertEquals(REFERENCE, response.getReference());

        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
    }

    @Test
    void testDeposit_VerifyBalanceIncreased() {
        // Mutation: Change add to subtract on balance calculation
        // This test will FAIL if mutation is applied
        
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        walletService.deposit(USER_ID, DEPOSIT_AMOUNT, REFERENCE);

        verify(walletRepository).save(argThat(w ->
                w.getBalance().compareTo(INITIAL_BALANCE.add(DEPOSIT_AMOUNT)) == 0
        ));
    }

    @Test
    void testDeposit_VerifyLedgerEntry() {
        // Mutation: Change LedgerType.CREDIT to DEBIT
        // This test will FAIL if mutation is applied
        
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        walletService.deposit(USER_ID, DEPOSIT_AMOUNT, REFERENCE);

        verify(ledgerEntryRepository).save(argThat(entry ->
                entry.getType() == LedgerType.CREDIT &&
                entry.getAmount().compareTo(DEPOSIT_AMOUNT) == 0 &&
                entry.getBalanceBefore().compareTo(INITIAL_BALANCE) == 0 &&
                entry.getBalanceAfter().compareTo(INITIAL_BALANCE.add(DEPOSIT_AMOUNT)) == 0 &&
                entry.getUserId().equals(USER_ID) &&
                entry.getReferenceId().equals(REFERENCE)
        ));
    }

    @Test
    void testDeposit_WalletNotFound_ThrowsException() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.deposit(USER_ID, DEPOSIT_AMOUNT, REFERENCE)
        );

        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    // ========== UPDATE STATUS TESTS - MUTATION CRITICAL ==========

    @Test
    void testUpdateStatus_SameStatus_NoChange() {
        // Mutation: Change == to != on status comparison
        // This test will FAIL if mutation is applied
        
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletService.WalletStatusResponse response = walletService.updateStatus(
                USER_ID, WalletStatus.ACTIVE, Role.ADMIN
        );

        assertNotNull(response);
        assertEquals(WalletStatus.ACTIVE, response.getWallet().getStatus());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testUpdateStatus_AdminFreeze_Success() {
        // Mutation: Remove the freeze condition
        // This test will FAIL if mutation is applied
        
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletService.WalletStatusResponse response = walletService.updateStatus(
                USER_ID, WalletStatus.FROZEN, Role.ADMIN
        );

        verify(walletRepository).save(argThat(w ->
                w.getStatus() == WalletStatus.FROZEN
        ));
    }

    @Test
    void testUpdateStatus_AdminUnfreeze_Success() {
        // Mutation: Change && to || on unfreeze condition
        // This test will FAIL if mutation is applied
        
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletService.WalletStatusResponse response = walletService.updateStatus(
                USER_ID, WalletStatus.ACTIVE, Role.ADMIN
        );

        verify(walletRepository).save(argThat(w ->
                w.getStatus() == WalletStatus.ACTIVE
        ));
    }

    @Test
    void testUpdateStatus_NonAdminFreeze_NoChange() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        // Non-admin cannot freeze, should throw exception
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.updateStatus(USER_ID, WalletStatus.FROZEN, Role.USER)
        );

        assertEquals("Invalid status transition for user role", exception.getMessage());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testUpdateStatus_WalletNotFound_ThrowsException() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.updateStatus(USER_ID, WalletStatus.FROZEN, Role.ADMIN)
        );

        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    @Test
    void testUpdateStatus_FrozenToActive_AdminOnly() {
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Admin can unfreeze
        WalletService.WalletStatusResponse adminResponse = walletService.updateStatus(
                USER_ID, WalletStatus.ACTIVE, Role.ADMIN
        );

        assertEquals(WalletStatus.ACTIVE, adminResponse.getWallet().getStatus());
        verify(walletRepository, times(1)).save(any());

        // Reset for user test
        reset(walletRepository);
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        // Non-admin cannot unfreeze from FROZEN to ACTIVE (different from INACTIVE to ACTIVE)
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                walletService.updateStatus(USER_ID, WalletStatus.ACTIVE, Role.USER)
        );

        assertEquals("Invalid status transition for user role", exception.getMessage());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testDeposit_LargeAmount_Success() {
        BigDecimal largeAmount = new BigDecimal("100000000.00");
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletService.WalletDepositResponse response = walletService.deposit(USER_ID, largeAmount, REFERENCE);

        assertEquals(largeAmount, response.getAmount());
        assertEquals(INITIAL_BALANCE.add(largeAmount), response.getNewBalance());
    }

    @Test
    void testDeposit_SmallDecimalAmount_Success() {
        BigDecimal smallAmount = new BigDecimal("0.01");
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletService.WalletDepositResponse response = walletService.deposit(USER_ID, smallAmount, REFERENCE);

        assertEquals(smallAmount, response.getAmount());
        assertEquals(INITIAL_BALANCE.add(smallAmount), response.getNewBalance());
    }
}
