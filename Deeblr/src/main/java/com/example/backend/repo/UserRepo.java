package com.example.backend.repo;

import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    //forces the calling code to handle the case where the user doesn't exist.
    Optional<User> findByUsername(String username);

    // search by the long JWT token
    //this is used for the Email Verification link.
    //when the user clicks the link with "?code=xyz", we use this to find who they are
    User findByVerificationToken(String token);
}