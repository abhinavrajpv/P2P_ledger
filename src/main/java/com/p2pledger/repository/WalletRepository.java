package com.p2pledger.repository;

import com.p2pledger.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    Iterable<Wallet> findByDailyLimitResetDateBefore(LocalDate date);
}
