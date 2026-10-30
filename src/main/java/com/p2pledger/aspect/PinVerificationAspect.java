package com.p2pledger.aspect;

import com.p2pledger.annotation.RequirePin;
import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PinVerificationAspect {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Around("@annotation(requirePin)")
    public Object verifyPin(ProceedingJoinPoint joinPoint, RequirePin requirePin) throws Throwable {
        Object[] args = joinPoint.getArgs();

        int userIdIndex = requirePin.userIdIndex();
        int pinIndex = requirePin.pinIndex();

        // Validate that the required arguments exist
        if (args.length <= userIdIndex || args.length <= pinIndex) {
            throw new IllegalArgumentException("Invalid PIN verification configuration: method does not have required parameters");
        }

        // Validate argument types
        if (!(args[userIdIndex] instanceof Long)) {
            throw new IllegalArgumentException("PIN verification requires Long userId parameter at position " + userIdIndex);
        }
        if (!(args[pinIndex] instanceof String)) {
            throw new IllegalArgumentException("PIN verification requires String pin parameter at position " + pinIndex);
        }

        Long userId = (Long) args[userIdIndex];
        String providedPin = (String) args[pinIndex];

        // Validate PIN format - must be exactly 4 digits
        if (providedPin == null || providedPin.length() != 4 || !providedPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("Invalid transaction PIN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String storedHash = user.getTransactionPin();
        if (storedHash == null || storedHash.isEmpty()) {
            throw new IllegalArgumentException("Invalid transaction PIN");
        }

        // Verify PIN using BCrypt hash comparison
        if (!passwordEncoder.matches(providedPin, storedHash)) {
            throw new IllegalArgumentException("Invalid transaction PIN");
        }

        return joinPoint.proceed();
    }
}
