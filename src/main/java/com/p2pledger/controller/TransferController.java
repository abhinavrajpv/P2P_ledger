package com.p2pledger.controller;

import com.p2pledger.model.Transfer;
import com.p2pledger.service.TransferService;
import com.p2pledger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final UserService userService;

    @GetMapping("/users/search")
    public ResponseEntity<List<UserService.UserSearchResult>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @PostMapping
    public ResponseEntity<Transfer> createTransfer(@RequestBody CreateTransferRequest request, Authentication authentication) {
        Long senderId = Long.parseLong(authentication.getName());
        Transfer transfer = transferService.initiateTransfer(
                senderId,
                request.getReceiverId(),
                request.getAmount(),
                request.getRequestId(),
                request.getPin()
        );
        return ResponseEntity.ok(transfer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transfer> getTransfer(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Transfer transfer = transferService.getTransferById(id);
        
        // Check if user is sender or receiver
        if (!transfer.getSenderId().equals(userId) && !transfer.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }
        
        return ResponseEntity.ok(transfer);
    }

    @GetMapping
    public ResponseEntity<Page<TransferService.TransferHistoryDto>> getTransactionHistory(
            Pageable pageable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<TransferService.TransferHistoryDto> result = transferService.getTransactionHistory(
                userId, pageable.getPageNumber(), pageable.getPageSize(), type, status, startDate, endDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<TransferService.TransferSummary> getSummary(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        TransferService.TransferSummary summary = transferService.getTransferSummary(userId);
        return ResponseEntity.ok(summary);
    }

    public static class CreateTransferRequest {
        private Long receiverId;
        private java.math.BigDecimal amount;
        private String requestId;
        private String pin;

        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
    }
}
