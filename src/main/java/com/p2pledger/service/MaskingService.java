package com.p2pledger.service;

import com.p2pledger.annotation.RequirePin;
import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaskingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MaskingStatus getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new MaskingStatus(user.isMaskingEnabled());
    }

    @Transactional
    @RequirePin(message = "Invalid transaction PIN", userIdIndex = 0, pinIndex = 1)
    public MaskingStatus enableMasking(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setMaskingEnabled(true);
        userRepository.save(user);

        return new MaskingStatus(true);
    }

    @Transactional
    @RequirePin(message = "Invalid transaction PIN", userIdIndex = 0, pinIndex = 1)
    public MaskingStatus disableMasking(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setMaskingEnabled(false);
        userRepository.save(user);

        return new MaskingStatus(false);
    }

    public static class MaskingStatus {
        private final boolean enabled;

        public MaskingStatus(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
