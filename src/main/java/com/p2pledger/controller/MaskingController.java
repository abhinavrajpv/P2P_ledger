package com.p2pledger.controller;

import com.p2pledger.service.MaskingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/masking")
@RequiredArgsConstructor
public class MaskingController {

    private final MaskingService maskingService;

    @GetMapping("/status")
    public ResponseEntity<MaskingService.MaskingStatus> getStatus(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(maskingService.getStatus(userId));
    }

    @PostMapping("/enable")
    public ResponseEntity<MaskingService.MaskingStatus> enableMasking(@RequestBody PinRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(maskingService.enableMasking(userId, request.getPin()));
    }

    @PostMapping("/disable")
    public ResponseEntity<MaskingService.MaskingStatus> disableMasking(@RequestBody PinRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(maskingService.disableMasking(userId, request.getPin()));
    }

    public static class PinRequest {
        private String pin;

        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
    }
}
