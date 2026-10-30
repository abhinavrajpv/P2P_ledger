package com.p2pledger.controller;

import com.p2pledger.model.Role;
import com.p2pledger.model.WalletStatus;
import com.p2pledger.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<WalletService.WalletBalanceResponse> getBalance(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletService.WalletDepositResponse> deposit(
            @RequestBody DepositRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(walletService.deposit(userId, request.getAmount(), request.getReference()));
    }

    @PatchMapping("/status")
    public ResponseEntity<WalletService.WalletStatusResponse> updateStatus(
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Role callerRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? Role.ADMIN : Role.USER;
        return ResponseEntity.ok(walletService.updateStatus(userId, request.getStatus(), callerRole));
    }

    public static class DepositRequest {
        private java.math.BigDecimal amount;
        private String reference;

        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
    }

    public static class StatusUpdateRequest {
        private WalletStatus status;

        public WalletStatus getStatus() { return status; }
        public void setStatus(WalletStatus status) { this.status = status; }
    }
}
