package com.p2pledger.service;

import com.p2pledger.model.User;
import com.p2pledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserSearchResult> searchUsers(String query) {
        List<User> users = userRepository.searchUsers(query);
        return users.stream()
                .map(user -> new UserSearchResult(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone()
                ))
                .collect(Collectors.toList());
    }

    public UserSearchResult getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserSearchResult(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone()
        );
    }
    
    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public static class UserSearchResult {
        private final Long id;
        private final String name;
        private final String email;
        private final String phone;

        public UserSearchResult(Long id, String name, String email, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }
}
