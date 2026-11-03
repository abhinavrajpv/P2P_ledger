
package com.p2pledger.service;

import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(USER_ID_1)
                .name("Abhinav")
                .email("abhinav@example.com")
                .phone("9876543210")
                .build();

        user2 = User.builder()
                .id(USER_ID_2)
                .name("Rahul")
                .email("rahul@example.com")
                .phone("9876543211")
                .build();
    }

    // ==========================================
    // SEARCH USERS
    // ==========================================

    @Test
    void testSearchUsers_WithMultipleResults() {
        when(userRepository.searchUsers("ab"))
                .thenReturn(List.of(user1, user2));

        List<UserService.UserSearchResult> result =
                userService.searchUsers("ab");

        assertEquals(2, result.size());

        assertEquals(USER_ID_1, result.get(0).getId());
        assertEquals("Abhinav", result.get(0).getName());
        assertEquals("abhinav@example.com", result.get(0).getEmail());
        assertEquals("9876543210", result.get(0).getPhone());

        assertEquals(USER_ID_2, result.get(1).getId());
        assertEquals("Rahul", result.get(1).getName());
        assertEquals("rahul@example.com", result.get(1).getEmail());
        assertEquals("9876543211", result.get(1).getPhone());

        verify(userRepository).searchUsers("ab");
    }

    @Test
    void testSearchUsers_WithOneResult() {
        when(userRepository.searchUsers("abhinav"))
                .thenReturn(List.of(user1));

        List<UserService.UserSearchResult> result =
                userService.searchUsers("abhinav");

        assertEquals(1, result.size());
        assertEquals("Abhinav", result.get(0).getName());
    }

    @Test
    void testSearchUsers_NoResults() {
        when(userRepository.searchUsers("unknown"))
                .thenReturn(List.of());

        List<UserService.UserSearchResult> result =
                userService.searchUsers("unknown");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).searchUsers("unknown");
    }

    @Test
    void testSearchUsers_NullQuery() {
        when(userRepository.searchUsers(null))
                .thenReturn(List.of());

        List<UserService.UserSearchResult> result =
                userService.searchUsers(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).searchUsers(null);
    }

    // ==========================================
    // GET USER BY ID
    // ==========================================

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(USER_ID_1))
                .thenReturn(Optional.of(user1));

        UserService.UserSearchResult result =
                userService.getUserById(USER_ID_1);

        assertNotNull(result);
        assertEquals(USER_ID_1, result.getId());
        assertEquals("Abhinav", result.getName());
        assertEquals("abhinav@example.com", result.getEmail());
        assertEquals("9876543210", result.getPhone());

        verify(userRepository).findById(USER_ID_1);
    }

    @Test
    void testGetUserById_UserNotFound() {
        when(userRepository.findById(USER_ID_1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(USER_ID_1)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(USER_ID_1);
    }

    // ==========================================
    // GET USER ENTITY BY ID
    // ==========================================

    @Test
    void testGetUserEntityById_Success() {
        when(userRepository.findById(USER_ID_1))
                .thenReturn(Optional.of(user1));

        User result = userService.getUserEntityById(USER_ID_1);

        assertSame(user1, result);

        verify(userRepository).findById(USER_ID_1);
    }

    @Test
    void testGetUserEntityById_UserNotFound() {
        when(userRepository.findById(USER_ID_1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserEntityById(USER_ID_1)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(USER_ID_1);
    }

    // ==========================================
    // USER SEARCH RESULT DTO
    // ==========================================

    @Test
    void testUserSearchResult_Getters() {
        UserService.UserSearchResult result =
                new UserService.UserSearchResult(
                        USER_ID_1,
                        "Abhinav",
                        "abhinav@example.com",
                        "9876543210"
                );

        assertEquals(USER_ID_1, result.getId());
        assertEquals("Abhinav", result.getName());
        assertEquals("abhinav@example.com", result.getEmail());
        assertEquals("9876543210", result.getPhone());
    }

    @Test
    void testUserSearchResult_NullValues() {
        UserService.UserSearchResult result =
                new UserService.UserSearchResult(null, null, null, null);

        assertNull(result.getId());
        assertNull(result.getName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());
    }
}
