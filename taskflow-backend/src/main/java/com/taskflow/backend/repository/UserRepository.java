package com.taskflow.backend.repository;

import com.taskflow.backend.model.Role;
import com.taskflow.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
}
