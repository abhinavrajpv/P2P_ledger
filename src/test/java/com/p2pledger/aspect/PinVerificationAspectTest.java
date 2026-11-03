
package com.p2pledger.aspect;

import com.p2pledger.annotation.RequirePin;
import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinVerificationAspectTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RequirePin requirePin;

    @InjectMocks
    private PinVerificationAspect pinVerificationAspect;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setTransactionPin("$2a$10$hashedPin");
    }

    @Test
    void testVerifyPin_Success() throws Throwable {
        Long userId = 1L;
        String pin = "1234";
        Object[] args = {userId, "other", "params", "here", pin};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(4);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(pin, testUser.getTransactionPin())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = pinVerificationAspect.verifyPin(joinPoint, requirePin);

        assertEquals("success", result);
        verify(joinPoint).proceed();
        verify(passwordEncoder).matches(pin, testUser.getTransactionPin());
    }

    @Test
    void testVerifyPin_InvalidUserIdIndex() throws Throwable {
        Object[] args = {1L, "1234"};

        when(requirePin.userIdIndex()).thenReturn(5);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertTrue(exception.getMessage().contains("Invalid PIN verification configuration"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_InvalidPinIndex() throws Throwable {
        Object[] args = {1L, "1234"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(5);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertTrue(exception.getMessage().contains("Invalid PIN verification configuration"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_InvalidUserIdType() throws Throwable {
        Object[] args = {"notALong", "1234"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertTrue(exception.getMessage().contains("Long userId parameter"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_InvalidPinType() throws Throwable {
        Object[] args = {1L, 1234};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertTrue(exception.getMessage().contains("String pin parameter"));
        verify(joinPoint, never()).proceed();
    }

  @Test
void testVerifyPin_NullPin() throws Throwable {
    // Arrange
    Object[] args = {1L, null};
    when(requirePin.userIdIndex()).thenReturn(0);
    when(requirePin.pinIndex()).thenReturn(1);
    when(joinPoint.getArgs()).thenReturn(args);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
    );
    assertEquals(
            "PIN verification requires String pin parameter at position 1",
            exception.getMessage()
    );
    verify(joinPoint, never()).proceed();
}

    @Test
    void testVerifyPin_InvalidPinLength_Short() throws Throwable {
        Object[] args = {1L, "123"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_InvalidPinLength_Long() throws Throwable {
        Object[] args = {1L, "12345"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_InvalidPinFormat_NonNumeric() throws Throwable {
        Object[] args = {1L, "abcd"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_UserNotFound() throws Throwable {
        Long userId = 999L;
        Object[] args = {userId, "1234"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("User not found", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_NullStoredHash() throws Throwable {
        testUser.setTransactionPin(null);

        Long userId = 1L;
        Object[] args = {userId, "1234"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_EmptyStoredHash() throws Throwable {
        testUser.setTransactionPin("");

        Long userId = 1L;
        Object[] args = {userId, "1234"};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void testVerifyPin_IncorrectPin() throws Throwable {
        Long userId = 1L;
        String wrongPin = "9999";
        Object[] args = {userId, wrongPin};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPin, testUser.getTransactionPin())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Invalid transaction PIN", exception.getMessage());
        verify(joinPoint, never()).proceed();
        verify(passwordEncoder).matches(wrongPin, testUser.getTransactionPin());
    }

    @Test
    void testVerifyPin_WithDifferentIndexes() throws Throwable {
        Long userId = 5L;
        String pin = "5678";
        Object[] args = {"param1", "param2", userId, "param4", "param5", pin};

        when(requirePin.userIdIndex()).thenReturn(2);
        when(requirePin.pinIndex()).thenReturn(5);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(pin, testUser.getTransactionPin())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("proceed_result");

        Object result = pinVerificationAspect.verifyPin(joinPoint, requirePin);

        assertEquals("proceed_result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void testVerifyPin_JoinPointThrowsException() throws Throwable {
        Long userId = 1L;
        String pin = "1234";
        Object[] args = {userId, pin};

        when(requirePin.userIdIndex()).thenReturn(0);
        when(requirePin.pinIndex()).thenReturn(1);
        when(joinPoint.getArgs()).thenReturn(args);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(pin, testUser.getTransactionPin())).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Business logic error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pinVerificationAspect.verifyPin(joinPoint, requirePin)
        );

        assertEquals("Business logic error", exception.getMessage());
    }
}
