package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repo.UserRepo;
import com.example.backend.service.EmailService;
import com.example.backend.security.jwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    //interface that performs a one-way transformation of a password to let the password be stored securely
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepo userRepo;
    @Autowired private EmailService emailService;
    @Autowired private jwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String password,
                                      @RequestParam String email) {
        //Check if username exists to prevent duplicates
        if (userRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username taken");
        }

        User user = new User();
        user.setUsername(username);

        // hash the password before saving using BCrypt.
        user.setPassword(passwordEncoder.encode(password));

        user.setEmail(email);

        // Create user as "Disabled" (0) Account is locked until they verify email.
        user.setEnabled(false);

        // Generate JWT Token To use as the verification code. it's unique and tied to the user.
        String token = jwtUtils.generateToken(username);
        user.setVerificationToken(token);

        userRepo.save(user);

        // Send Email
        try {
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending email " + e.getMessage());
        }

        return ResponseEntity.ok("Registration successful. Check email to verify.");
    }

    //EMAIL VERIFICATION
    @GetMapping("/verify")
    public String verifyUser(@RequestParam("code") String token) {
        //Validate that the token hasn't been tampered with or expired.
        if (!jwtUtils.validateToken(token)) {
            return "<html><body><h1>Error</h1><p>Invalid or expired token.</p></body></html>";
        }

        User user = userRepo.findByVerificationToken(token);

        if (user == null) {
            return "<html><body><h1>Error</h1><p>User not found.</p></body></html>";
        }

        // Check if already enabled (1)
        if (user.isEnabled()) {
            return "<html><body><h1>Already Verified</h1></body></html>";
        }

        // ACTIVATE ACCOUNT clear the token and enable the user.
        user.setVerificationToken(null);
        user.setEnabled(true);
        userRepo.save(user);

        return "<html><body><h1>Success!</h1><p>Account verified.</p></body></html>";
    }

    //LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        User user = userRepo.findByUsername(username).orElse(null);
        // compares the plain-text password from the user with the BCrypt hash in the database.
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {

            // check if verified
            if (!user.isEnabled()) {
                return ResponseEntity.status(403).body("Account not verified. Check email.");
            }

            // GENERATE THE session token for the next 10 hours
            String token = jwtUtils.generateToken(user.getUsername());

            // return a JSON object (Map) containing the token.
            // frontend stores this in localStorage
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername()
            ));
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}