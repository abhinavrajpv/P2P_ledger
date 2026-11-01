package com.p2pledger.service;

import com.p2pledger.model.LoginAttempts;
import com.p2pledger.model.Role;
import com.p2pledger.model.User;
import com.p2pledger.model.Wallet;
import com.p2pledger.repository.LoginAttemptsRepository;
import com.p2pledger.repository.UserRepository;
import com.p2pledger.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;
    private static final int TRANSACTION_PIN_LENGTH = 4;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LoginAttemptsRepository loginAttemptsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register(String name, String email, String phone, String password, String transactionPin) {
        // Validate inputs
        validateEmail(email);
        validatePhone(phone);
        validatePassword(password);
        validateTransactionPin(transactionPin);

        // Check if email or phone already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone already registered");
        }

        // Hash password and transaction PIN
        String hashedPassword = passwordEncoder.encode(password);
        String hashedTransactionPin = passwordEncoder.encode(transactionPin);

        // Create user with USER role only (never trust client role)
        User user = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .password(hashedPassword)
                .transactionPin(hashedTransactionPin)
                .role(Role.USER)
                .maskingEnabled(false)
                .failedLoginCount(0)
                .lockedUntil(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Create wallet with zero balance
        Wallet wallet = Wallet.builder()
                .userId(savedUser.getId())
                .balance(java.math.BigDecimal.ZERO)
                .todaySpending(java.math.BigDecimal.ZERO)
                .dailyLimitResetDate(java.time.LocalDate.now())
                .status(com.p2pledger.model.WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        walletRepository.save(wallet);

        return savedUser;
    }

    public User login(String identifier, String password) {
        // Find user by email or phone
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElse(null));

        // Check if user exists
        if (user == null) {
            // Log failed login attempt (user not found)
            logLoginAttempt(null, identifier, false, "User not found");
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            // Log failed login attempt (account locked)
            logLoginAttempt(user.getId(), identifier, false, "Account locked");
            throw new IllegalArgumentException("Account is locked until " + user.getLockedUntil());
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Increment failed login counter
            int newFailedCount = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(newFailedCount);

            // Lock account if threshold reached
            if (newFailedCount >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            }

            userRepository.save(user);
            
            // Log failed login attempt (invalid password)
            logLoginAttempt(user.getId(), identifier, false, "Invalid password");
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Reset failed login counter on successful login
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());

        // Log successful login attempt
        logLoginAttempt(user.getId(), identifier, true, "Login successful");

        return userRepository.save(user);
    }

    private void logLoginAttempt(Long userId, String identifier, boolean successful, String source) {
        LoginAttempts loginAttempt = LoginAttempts.builder()
                .userId(userId)
                .identifier(identifier)
                .attemptedAt(LocalDateTime.now())
                .successful(successful)
                .source(source)
                .build();
        
        loginAttemptsRepository.save(loginAttempt);
    }

    private void validateEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!Pattern.matches(emailRegex, email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePhone(String phone) {
        // Validate phone number format (adjust based on requirements)
        String phoneRegex = "^\\+?[1-9]\\d{1,14}$";
        if (!Pattern.matches(phoneRegex, phone)) {
            throw new IllegalArgumentException("Invalid phone format");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    private void validateTransactionPin(String pin) {
        if (pin == null || pin.length() != TRANSACTION_PIN_LENGTH) {
            throw new IllegalArgumentException("Transaction PIN must be exactly 4 digits");
        }
        if (!pin.matches("\\d+")) {
            throw new IllegalArgumentException("Transaction PIN must contain only digits");
        }
    }
}
