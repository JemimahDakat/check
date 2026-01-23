package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repo.UserRepo;
import com.example.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController // Returns JSON, not HTML
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepo userRepo;
    @Autowired private JwtUtils jwtUtils;
    // @Autowired private PasswordEncoder passwordEncoder; // Recommended for production

    // LOGIN Endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        // 1. Find user
        User user = userRepo.findByUsername(username).orElse(null);

        // 2. Validate Password (Simple string check for now)
        if (user != null && user.getPassword().equals(password)) {

            // 3. Check if Verified (Optional, based on your email logic)
            if (!user.isEnabled()) {
                return ResponseEntity.status(403).body("Account not verified. Please check your email.");
            }

            // 4. Generate JWT
            String token = jwtUtils.generateToken(username);

            // 5. Return Token in JSON
            return ResponseEntity.ok(Map.of("token", token, "username", username));
        }

        return ResponseEntity.status(401).body("Invalid username or password");
    }

    // REGISTER Endpoint
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String password,
                                      @RequestParam String email) {

        if (userRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Remember to encode this in real app!
        user.setEmail(email);
        user.setEnabled(true); // Set to 'false' if you want email verification enabled

        userRepo.save(user);

        return ResponseEntity.ok("User registered successfully");
    }
}