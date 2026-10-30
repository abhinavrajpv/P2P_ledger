package com.p2pledger.repository;

import com.p2pledger.model.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findBySenderId(Long senderId);

    List<Transfer> findByReceiverId(Long receiverId);

    Page<Transfer> findBySenderId(Long senderId, Pageable pageable);

    Page<Transfer> findByReceiverId(Long receiverId, Pageable pageable);

    Optional<Transfer> findByRequestId(String requestId);

    List<Transfer> findByStatus(String status);

    Page<Transfer> findByStatus(String status, Pageable pageable);

    List<Transfer> findBySenderIdAndReceiverIdAndAmountAndCreatedAtAfter(
            Long senderId, Long receiverId, java.math.BigDecimal amount, LocalDateTime after);

    Page<Transfer> findBySenderIdAndStatus(Long senderId, String status, Pageable pageable);

    Page<Transfer> findBySenderIdAndCreatedAtBetween(Long senderId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Transfer> findBySenderIdAndCreatedAtAfter(Long senderId, LocalDateTime startDate, Pageable pageable);

    Page<Transfer> findBySenderIdAndCreatedAtBefore(Long senderId, LocalDateTime endDate, Pageable pageable);

    Page<Transfer> findByReceiverIdAndStatus(Long receiverId, String status, Pageable pageable);

    Page<Transfer> findByReceiverIdAndCreatedAtBetween(Long receiverId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Transfer> findByReceiverIdAndCreatedAtAfter(Long receiverId, LocalDateTime startDate, Pageable pageable);

    Page<Transfer> findByReceiverIdAndCreatedAtBefore(Long receiverId, LocalDateTime endDate, Pageable pageable);

    Page<Transfer> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Transfer> findByCreatedAtAfter(LocalDateTime startDate, Pageable pageable);

    Page<Transfer> findByCreatedAtBefore(LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.senderId = :userId OR t.receiverId = :userId")
    Page<Transfer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE (t.senderId = :userId OR t.receiverId = :userId) " +
           "ORDER BY t.createdAt DESC")
    Page<Transfer> findByUserIdWithFilters(@Param("userId") Long userId,
                                           Pageable pageable);
}
