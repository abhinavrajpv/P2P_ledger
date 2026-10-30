package com.p2pledger.service;

import com.p2pledger.model.LedgerEntry;
import com.p2pledger.model.LedgerType;
import com.p2pledger.model.Role;
import com.p2pledger.model.Wallet;
import com.p2pledger.model.WalletStatus;
import com.p2pledger.repository.LedgerEntryRepository;
import com.p2pledger.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletBalanceResponse getBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        return WalletBalanceResponse.builder()
                .balance(wallet.getBalance())
                .todaySpending(wallet.getTodaySpending())
                .dailyLimit(BigDecimal.valueOf(50000))
                .status(wallet.getStatus())
                .build();
    }

    @Transactional
    public WalletDepositResponse deposit(Long userId, BigDecimal amount, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        BigDecimal oldBalance = wallet.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        wallet.setBalance(newBalance);

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .transferId(null)
                .userId(userId)
                .type(LedgerType.CREDIT)
                .amount(amount)
                .balanceBefore(oldBalance)
                .balanceAfter(newBalance)
                .referenceId(reference)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(ledgerEntry);

        walletRepository.save(wallet);

        return WalletDepositResponse.builder()
                .id(wallet.getId())
                .amount(amount)
                .newBalance(newBalance)
                .reference(reference)
                .build();
    }

    @Transactional
    public WalletStatusResponse updateStatus(Long userId, WalletStatus status, Role callerRole) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        WalletStatus currentStatus = wallet.getStatus();

        if (currentStatus == status) {
            return WalletStatusResponse.builder()
                    .wallet(wallet)
                    .build();
        }

        // Admin can freeze any wallet to FROZEN and unfreeze FROZEN to ACTIVE
        if (callerRole == Role.ADMIN) {
            if (status == WalletStatus.ACTIVE && currentStatus == WalletStatus.FROZEN) {
                wallet.setStatus(WalletStatus.ACTIVE);
                walletRepository.save(wallet);
                return WalletStatusResponse.builder()
                        .wallet(wallet)
                        .build();
            }
            if (status == WalletStatus.FROZEN) {
                wallet.setStatus(WalletStatus.FROZEN);
                walletRepository.save(wallet);
                return WalletStatusResponse.builder()
                        .wallet(wallet)
                        .build();
            }
        }

        // User can only change between ACTIVE and INACTIVE
        if (callerRole == Role.USER) {
            if ((currentStatus == WalletStatus.ACTIVE && status == WalletStatus.INACTIVE) ||
                (currentStatus == WalletStatus.INACTIVE && status == WalletStatus.ACTIVE)) {
                wallet.setStatus(status);
                walletRepository.save(wallet);
                return WalletStatusResponse.builder()
                        .wallet(wallet)
                        .build();
            }
        }

        throw new IllegalArgumentException("Invalid status transition for user role");
    }

    public static class WalletBalanceResponse {
        private final BigDecimal balance;
        private final BigDecimal todaySpending;
        private final BigDecimal dailyLimit;
        private final WalletStatus status;

        public WalletBalanceResponse(BigDecimal balance, BigDecimal todaySpending, BigDecimal dailyLimit, WalletStatus status) {
            this.balance = balance;
            this.todaySpending = todaySpending;
            this.dailyLimit = dailyLimit;
            this.status = status;
        }

        public BigDecimal getBalance() { return balance; }
        public BigDecimal getTodaySpending() { return todaySpending; }
        public BigDecimal getDailyLimit() { return dailyLimit; }
        public WalletStatus getStatus() { return status; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BigDecimal balance;
            private BigDecimal todaySpending;
            private BigDecimal dailyLimit;
            private WalletStatus status;

            public Builder balance(BigDecimal balance) {
                this.balance = balance;
                return this;
            }

            public Builder todaySpending(BigDecimal todaySpending) {
                this.todaySpending = todaySpending;
                return this;
            }

            public Builder dailyLimit(BigDecimal dailyLimit) {
                this.dailyLimit = dailyLimit;
                return this;
            }

            public Builder status(WalletStatus status) {
                this.status = status;
                return this;
            }

            public WalletBalanceResponse build() {
                return new WalletBalanceResponse(balance, todaySpending, dailyLimit, status);
            }
        }
    }

    public static class WalletDepositResponse {
        private final Long id;
        private final BigDecimal amount;
        private final BigDecimal newBalance;
        private final String reference;

        public WalletDepositResponse(Long id, BigDecimal amount, BigDecimal newBalance, String reference) {
            this.id = id;
            this.amount = amount;
            this.newBalance = newBalance;
            this.reference = reference;
        }

        public Long getId() { return id; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getNewBalance() { return newBalance; }
        public String getReference() { return reference; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long id;
            private BigDecimal amount;
            private BigDecimal newBalance;
            private String reference;

            public Builder id(Long id) {
                this.id = id;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            public Builder newBalance(BigDecimal newBalance) {
                this.newBalance = newBalance;
                return this;
            }

            public Builder reference(String reference) {
                this.reference = reference;
                return this;
            }

            public WalletDepositResponse build() {
                return new WalletDepositResponse(id, amount, newBalance, reference);
            }
        }
    }

    public static class WalletStatusResponse {
        private final Wallet wallet;

        public WalletStatusResponse(Wallet wallet) {
            this.wallet = wallet;
        }

        public Wallet getWallet() { return wallet; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Wallet wallet;

            public Builder wallet(Wallet wallet) {
                this.wallet = wallet;
                return this;
            }

            public WalletStatusResponse build() {
                return new WalletStatusResponse(wallet);
            }
        }
    }
}
