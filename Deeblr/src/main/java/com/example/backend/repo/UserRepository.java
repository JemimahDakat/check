package com.example.backend.repo;

import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query method to check if a user exists
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    //finds username for login
    Optional<User> findByUsername(String username);
}