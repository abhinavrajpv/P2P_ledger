package com.p2pledger.controller;

import com.p2pledger.model.*;
import com.p2pledger.service.AdminService;
import com.p2pledger.service.TransferService;
import com.p2pledger.service.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TransferService transferService;
    private final ScheduledJobService scheduledJobService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminService.DashboardData> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(adminService.getDashboardData());
    }
    
    @GetMapping("/users")
    public ResponseEntity<Page<AdminService.UserWithWallet>> getAllUsers(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(adminService.getAllUsersWithWallet(pageable));
    }
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminService.UserDetail> getUserDetail(@PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }
    
    @PostMapping("/users/{userId}/freeze")
    public ResponseEntity<Map<String, String>> freezeUserWallet(@PathVariable Long userId, Authentication authentication) {
        adminService.freezeUserWallet(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User wallet frozen successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/users/{userId}/unfreeze")
    public ResponseEntity<Map<String, String>> unfreezeUserWallet(@PathVariable Long userId, Authentication authentication) {
        adminService.unfreezeUserWallet(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User wallet unfrozen successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/settlements/summary")
    public ResponseEntity<AdminService.SettlementSummary> getSettlementSummary(Authentication authentication) {
        return ResponseEntity.ok(adminService.getSettlementSummary());
    }
    
    @GetMapping("/reconciliation/today")
    public ResponseEntity<AdminService.ReconciliationStatus> getTodayReconciliation(Authentication authentication) {
        return ResponseEntity.ok(adminService.getTodayReconciliation());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<Transfer>> getTransactions(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            Authentication authentication) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        return ResponseEntity.ok(adminService.getTransactionsWithFilters(pageable, status, senderId, receiverId, 
                startDateTime, endDateTime, minAmount, maxAmount));
    }

    @GetMapping("/settlements")
    public ResponseEntity<Page<Transfer>> getPendingSettlements(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(adminService.getPendingSettlements(pageable));
    }

    @PostMapping("/settlements/process")
    public ResponseEntity<Transfer> processSettlement(@RequestBody ProcessSettlementRequest request, Authentication authentication) {
        return ResponseEntity.ok(adminService.processSettlement(request.getTransferId()));
    }

    @GetMapping("/fraud")
    public ResponseEntity<Page<FraudAlert>> getFlaggedTransactions(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(adminService.getFlaggedTransactions(pageable));
    }

    @PostMapping("/fraud/{id}/approve")
    public ResponseEntity<Transfer> approveTransaction(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(adminService.approveTransaction(id));
    }

    @PostMapping("/fraud/{id}/refund")
    public ResponseEntity<Transfer> refundTransaction(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(adminService.refundTransaction(id));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<ScheduledJobLog>> getJobExecutionHistory(
            @RequestParam(required = false) String jobName,
            Authentication authentication) {
        return ResponseEntity.ok(scheduledJobService.getJobExecutionHistory(jobName));
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<java.util.List<DailySummary>> getReconciliationReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.getReconciliationReport(startDate, endDate));
    }

    public static class ProcessSettlementRequest {
        private Long transferId;

        public Long getTransferId() { return transferId; }
        public void setTransferId(Long transferId) { this.transferId = transferId; }
    }
}
