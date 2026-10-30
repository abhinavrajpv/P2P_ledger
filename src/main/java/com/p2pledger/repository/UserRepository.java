package com.p2pledger.repository;

import com.p2pledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Iterable<User> findByFailedLoginCountGreaterThan(int failedLoginCount);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:query% OR u.phone LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);
}
