package com.p2pledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_summary")
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    // Transfer statistics - initiated (all statuses)
    @Column(name = "total_transfers_initiated", nullable = false)
    private Long totalTransfersInitiated;

    @Column(name = "total_amount_initiated", nullable = false)
    private BigDecimal totalAmountInitiated;

    // Transfer statistics - settled (successful only)
    @Column(name = "total_transfers_settled", nullable = false)
    private Long totalTransfersSettled;

    @Column(name = "total_amount_settled", nullable = false)
    private BigDecimal totalAmountSettled;

    // Additional statistics
    @Column(name = "fraud_alerts_raised", nullable = false)
    private Long fraudAlertsRaised;

    @Column(name = "new_user_registrations", nullable = false)
    private Long newUserRegistrations;

    @Column(name = "pending_settlement_count", nullable = false)
    private Long pendingSettlementCount;

    @Column(name = "pending_settlement_amount", nullable = false)
    private BigDecimal pendingSettlementAmount;

    // Reconciliation data
    @Column(name = "total_debits", nullable = false)
    private BigDecimal totalDebits;

    @Column(name = "total_credits", nullable = false)
    private BigDecimal totalCredits;

    @Column(name = "debit_count", nullable = false)
    private Long debitCount;

    @Column(name = "credit_count", nullable = false)
    private Long creditCount;

    @Column(name = "reconciliation_balanced", nullable = false)
    private Boolean reconciliationBalanced;

    @Column(name = "reconciliation_mismatch", nullable = false)
    private BigDecimal reconciliationMismatch;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
