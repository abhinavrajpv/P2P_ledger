
package com.p2pledger.service;

import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaskingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private MaskingService maskingService;

    private User user;

    private static final Long USER_ID = 1L;
    private static final String PIN = "1234";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .name("Test User")
                .email("test@example.com")
                .maskingEnabled(false)
                .build();
    }

    // ==========================================
    // GET STATUS
    // ==========================================

    @Test
    void testGetStatus_MaskingEnabled() {
        user.setMaskingEnabled(true);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        MaskingService.MaskingStatus result =
                maskingService.getStatus(USER_ID);

        assertTrue(result.isEnabled());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void testGetStatus_MaskingDisabled() {
        user.setMaskingEnabled(false);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        MaskingService.MaskingStatus result =
                maskingService.getStatus(USER_ID);

        assertFalse(result.isEnabled());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void testGetStatus_UserNotFound() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> maskingService.getStatus(USER_ID)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // ENABLE MASKING
    // ==========================================

    @Test
    void testEnableMasking_Success() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaskingService.MaskingStatus result =
                maskingService.enableMasking(USER_ID, PIN);

        assertTrue(result.isEnabled());
        assertTrue(user.isMaskingEnabled());

        verify(userRepository).findById(USER_ID);
        verify(userRepository).save(user);
    }

    @Test
    void testEnableMasking_AlreadyEnabled() {
        user.setMaskingEnabled(true);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaskingService.MaskingStatus result =
                maskingService.enableMasking(USER_ID, PIN);

        assertTrue(result.isEnabled());
        assertTrue(user.isMaskingEnabled());

        verify(userRepository).save(user);
    }

    @Test
    void testEnableMasking_UserNotFound() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> maskingService.enableMasking(USER_ID, PIN)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // DISABLE MASKING
    // ==========================================

    @Test
    void testDisableMasking_Success() {
        user.setMaskingEnabled(true);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaskingService.MaskingStatus result =
                maskingService.disableMasking(USER_ID, PIN);

        assertFalse(result.isEnabled());
        assertFalse(user.isMaskingEnabled());

        verify(userRepository).findById(USER_ID);
        verify(userRepository).save(user);
    }

    @Test
    void testDisableMasking_AlreadyDisabled() {
        user.setMaskingEnabled(false);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaskingService.MaskingStatus result =
                maskingService.disableMasking(USER_ID, PIN);

        assertFalse(result.isEnabled());
        assertFalse(user.isMaskingEnabled());

        verify(userRepository).save(user);
    }

    @Test
    void testDisableMasking_UserNotFound() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> maskingService.disableMasking(USER_ID, PIN)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // MASKING STATUS DTO
    // ==========================================

    @Test
    void testMaskingStatus_Enabled() {
        MaskingService.MaskingStatus status =
                new MaskingService.MaskingStatus(true);

        assertTrue(status.isEnabled());
    }

    @Test
    void testMaskingStatus_Disabled() {
        MaskingService.MaskingStatus status =
                new MaskingService.MaskingStatus(false);

        assertFalse(status.isEnabled());
    }
}
