package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repo.UserRepo;
import com.example.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepo userRepo;
    @Autowired private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String password,
                                      @RequestParam String email) {

        if (userRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // In production, encrypt this!
        user.setEmail(email);
        user.setEnabled(false); //User is DISABLED until they click the link

        // Generate random code
        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);

        userRepo.save(user);

        try {
            emailService.sendVerificationEmail(email, randomCode);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending email: " + e.getMessage());
        }

        return ResponseEntity.ok("Registration successful. Please check your email to verify.");
    }

    // handles the link click
    @GetMapping("/verify")
    public String verifyUser(@RequestParam("code") String code) {
        User user = userRepo.findByVerificationCode(code);

        if (user == null || user.isEnabled()) {
            return "<html><body><h1>Verification Failed</h1><p>Invalid code or already verified.</p></body></html>";
        } else {
            user.setVerificationCode(null); // Clear code
            user.setEnabled(true);          // Enable account
            userRepo.save(user);
            return "<html><body><h1>Verification Successful!</h1><p>You can now <a href='login.html'>Login here</a></p></body></html>";
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        User user = userRepo.findByUsername(username).orElse(null);

        if (user != null && user.getPassword().equals(password)) {
            if (!user.isEnabled()) {
                return ResponseEntity.status(403).body("Account not verified. Please check your email and spam folder.");
            }
            return ResponseEntity.ok("Login Successful");
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}