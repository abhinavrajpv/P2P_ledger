package com.p2pledger.repository;

import com.p2pledger.model.LoginAttempts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptsRepository extends JpaRepository<LoginAttempts, Long> {

    List<LoginAttempts> findByUserIdOrderByAttemptedAtDesc(Long userId);

    List<LoginAttempts> findByIdentifierOrderByAttemptedAtDesc(String identifier);

    List<LoginAttempts> findByAttemptedAtAfter(LocalDateTime timestamp);

    List<LoginAttempts> findBySuccessfulFalseOrderByAttemptedAtDesc();
}
