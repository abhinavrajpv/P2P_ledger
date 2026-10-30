package com.p2pledger.controller;

import com.p2pledger.service.AuthService;
import com.p2pledger.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        authService.register(request.getName(), request.getEmail(), request.getPhone(), request.getPassword(), request.getTransactionPin());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        com.p2pledger.model.User user = authService.login(request.getIdentifier(), request.getPassword());
        
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        int expiresIn = 3600;
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresIn", expiresIn);
        response.put("user", Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "role", user.getRole().name()
        ));
        return ResponseEntity.ok(response);
    }

    public static class RegisterRequest {
        private String name;
        private String email;
        private String phone;
        private String password;
        private String transactionPin;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTransactionPin() { return transactionPin; }
        public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }
    }

    public static class LoginRequest {
        private String identifier;
        private String password;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
