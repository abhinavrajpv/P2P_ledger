package com.p2pledger.service;
import com.p2pledger.model.LoginAttempts;
import com.p2pledger.model.Role;
import com.p2pledger.model.User;
import com.p2pledger.model.Wallet;
import com.p2pledger.repository.LoginAttemptsRepository;
import com.p2pledger.repository.UserRepository;
import com.p2pledger.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LoginAttemptsRepository loginAttemptsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    private static final String VALID_NAME = "Test User";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PHONE = "9876543210";
    private static final String VALID_PASSWORD = "Password123";
    private static final String VALID_PIN = "1234";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName(VALID_NAME);
        testUser.setEmail(VALID_EMAIL);
        testUser.setPhone(VALID_PHONE);
        testUser.setPassword("hashedPassword");
        testUser.setTransactionPin("hashedPin");
        testUser.setRole(Role.USER);
        testUser.setFailedLoginCount(0);
        testUser.setLockedUntil(null);
    }

    // -------------------------------------------------------
    // REGISTER TESTS
    // -------------------------------------------------------

    @Test
    void testRegister_Success() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.empty());

        when(userRepository.findByPhone(VALID_PHONE))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(VALID_PASSWORD))
                .thenReturn("hashedPassword");

        when(passwordEncoder.encode(VALID_PIN))
                .thenReturn("hashedPin");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        User result = authService.register(
                VALID_NAME,
                VALID_EMAIL,
                VALID_PHONE,
                VALID_PASSWORD,
                VALID_PIN
        );

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(VALID_NAME, result.getName());
        assertEquals(VALID_EMAIL, result.getEmail());
        assertEquals(VALID_PHONE, result.getPhone());
        assertEquals("hashedPassword", result.getPassword());
        assertEquals("hashedPin", result.getTransactionPin());
        assertEquals(Role.USER, result.getRole());
        assertEquals(0, result.getFailedLoginCount());
        assertFalse(result.isMaskingEnabled());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(passwordEncoder).encode(VALID_PASSWORD);
        verify(passwordEncoder).encode(VALID_PIN);
        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testRegister_EmailAlreadyExists() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        VALID_NAME, VALID_EMAIL, VALID_PHONE,
                        VALID_PASSWORD, VALID_PIN
                )
        );

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testRegister_PhoneAlreadyExists() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.empty());

        when(userRepository.findByPhone(VALID_PHONE))
                .thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        VALID_NAME, VALID_EMAIL, VALID_PHONE,
                        VALID_PASSWORD, VALID_PIN
                )
        );

        assertEquals("Phone already registered", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    // -------------------------------------------------------
    // EMAIL VALIDATION TESTS
    // -------------------------------------------------------

    @Test
    void testRegister_InvalidEmail() {

        assertRegisterFails(
                "invalid-email",
                VALID_PHONE,
                VALID_PASSWORD,
                VALID_PIN,
                "Invalid email format"
        );
    }

    // -------------------------------------------------------
    // PHONE VALIDATION TESTS
    // -------------------------------------------------------

    @Test
    void testRegister_InvalidPhone() {

        assertRegisterFails(
                VALID_EMAIL,
                "012345",
                VALID_PASSWORD,
                VALID_PIN,
                "Invalid phone format"
        );
    }

    // -------------------------------------------------------
    // PASSWORD VALIDATION TESTS
    // -------------------------------------------------------

    @Test
    void testRegister_NullPassword() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                null,
                VALID_PIN,
                "Password must be at least 8 characters"
        );
    }

    @Test
    void testRegister_PasswordTooShort() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                "Ab1",
                VALID_PIN,
                "Password must be at least 8 characters"
        );
    }

    @Test
    void testRegister_PasswordWithoutUppercase() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                "password123",
                VALID_PIN,
                "Password must contain at least one uppercase letter"
        );
    }

    @Test
    void testRegister_PasswordWithoutLowercase() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                "PASSWORD123",
                VALID_PIN,
                "Password must contain at least one lowercase letter"
        );
    }

    @Test
    void testRegister_PasswordWithoutDigit() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                "PasswordOnly",
                VALID_PIN,
                "Password must contain at least one digit"
        );
    }

    // -------------------------------------------------------
    // TRANSACTION PIN VALIDATION TESTS
    // -------------------------------------------------------

    @Test
    void testRegister_NullTransactionPin() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                VALID_PASSWORD,
                null,
                "Transaction PIN must be exactly 4 digits"
        );
    }

    @Test
    void testRegister_TransactionPinWrongLength() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                VALID_PASSWORD,
                "123",
                "Transaction PIN must be exactly 4 digits"
        );
    }

    @Test
    void testRegister_TransactionPinContainsNonDigits() {

        assertRegisterFails(
                VALID_EMAIL,
                VALID_PHONE,
                VALID_PASSWORD,
                "12A4",
                "Transaction PIN must contain only digits"
        );
    }

    // -------------------------------------------------------
    // LOGIN TESTS
    // -------------------------------------------------------

    @Test
    void testLogin_UserNotFound() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.empty());

        when(userRepository.findByPhone(VALID_EMAIL))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        assertEquals("Invalid credentials", exception.getMessage());

        verify(loginAttemptsRepository).save(any(LoginAttempts.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_AccountLocked() {

        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        assertTrue(exception.getMessage().startsWith("Account is locked until"));

        verify(loginAttemptsRepository).save(any(LoginAttempts.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_InvalidPassword() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals(1, testUser.getFailedLoginCount());
        assertNull(testUser.getLockedUntil());

        verify(userRepository).save(testUser);
        verify(loginAttemptsRepository).save(any(LoginAttempts.class));
    }

    @Test
    void testLogin_FifthFailedAttempt_LocksAccount() {

        testUser.setFailedLoginCount(4);

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals(5, testUser.getFailedLoginCount());
        assertNotNull(testUser.getLockedUntil());
        assertTrue(testUser.getLockedUntil().isAfter(LocalDateTime.now()));

        verify(userRepository).save(testUser);
        verify(loginAttemptsRepository).save(any(LoginAttempts.class));
    }

    @Test
    void testLogin_Success() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(true);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.login(VALID_EMAIL, VALID_PASSWORD);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(0, result.getFailedLoginCount());
        assertNull(result.getLockedUntil());
        assertNotNull(result.getUpdatedAt());

        verify(passwordEncoder).matches(VALID_PASSWORD, "hashedPassword");
        verify(userRepository).save(testUser);
        verify(loginAttemptsRepository).save(any(LoginAttempts.class));
    }

    @Test
    void testLogin_SuccessWithExpiredLock() {

        testUser.setLockedUntil(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(true);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.login(VALID_EMAIL, VALID_PASSWORD);

        assertNotNull(result);
        assertNull(result.getLockedUntil());
        assertEquals(0, result.getFailedLoginCount());

        verify(userRepository).save(testUser);
    }

    // -------------------------------------------------------
    // LOGIN ATTEMPT LOGGING TESTS
    // -------------------------------------------------------

    @Test
    void testLogin_UserNotFound_LogsFailedAttempt() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.empty());

        when(userRepository.findByPhone(VALID_EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        ArgumentCaptor<LoginAttempts> captor =
                ArgumentCaptor.forClass(LoginAttempts.class);

        verify(loginAttemptsRepository).save(captor.capture());

        LoginAttempts attempt = captor.getValue();

        assertNull(attempt.getUserId());
        assertEquals(VALID_EMAIL, attempt.getIdentifier());
        assertFalse(attempt.isSuccessful());
        assertEquals("User not found", attempt.getSource());
        assertNotNull(attempt.getAttemptedAt());
    }

    @Test
    void testLogin_AccountLocked_LogsFailedAttempt() {

        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        ArgumentCaptor<LoginAttempts> captor =
                ArgumentCaptor.forClass(LoginAttempts.class);

        verify(loginAttemptsRepository).save(captor.capture());

        LoginAttempts attempt = captor.getValue();

        assertEquals(1L, attempt.getUserId());
        assertEquals(VALID_EMAIL, attempt.getIdentifier());
        assertFalse(attempt.isSuccessful());
        assertEquals("Account locked", attempt.getSource());
    }

    @Test
    void testLogin_InvalidPassword_LogsFailedAttempt() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(VALID_EMAIL, VALID_PASSWORD)
        );

        ArgumentCaptor<LoginAttempts> captor =
                ArgumentCaptor.forClass(LoginAttempts.class);

        verify(loginAttemptsRepository).save(captor.capture());

        LoginAttempts attempt = captor.getValue();

        assertEquals(1L, attempt.getUserId());
        assertEquals(VALID_EMAIL, attempt.getIdentifier());
        assertFalse(attempt.isSuccessful());
        assertEquals("Invalid password", attempt.getSource());
    }

    @Test
    void testLogin_Success_LogsSuccessfulAttempt() {

        when(userRepository.findByEmail(VALID_EMAIL))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(VALID_PASSWORD, "hashedPassword"))
                .thenReturn(true);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.login(VALID_EMAIL, VALID_PASSWORD);

        ArgumentCaptor<LoginAttempts> captor =
                ArgumentCaptor.forClass(LoginAttempts.class);

        verify(loginAttemptsRepository).save(captor.capture());

        LoginAttempts attempt = captor.getValue();

        assertEquals(1L, attempt.getUserId());
        assertEquals(VALID_EMAIL, attempt.getIdentifier());
        assertTrue(attempt.isSuccessful());
        assertEquals("Login successful", attempt.getSource());
    }

    // -------------------------------------------------------
    // HELPER METHOD
    // -------------------------------------------------------

    private void assertRegisterFails(
            String email,
            String phone,
            String password,
            String pin,
            String expectedMessage
    ) {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        VALID_NAME,
                        email,
                        phone,
                        password,
                        pin
                )
        );

        assertEquals(expectedMessage, exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}