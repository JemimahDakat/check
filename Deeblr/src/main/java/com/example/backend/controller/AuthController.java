package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repo.UserRepo;
import com.example.backend.service.EmailService;
import com.example.backend.security.jwtUtils; // utility class for generating and validating JWT tokens
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; //HTTP responses 403 eg
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map; //build a simple key-value JSON response

// requests are written to HTTP response body in json not
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

        // Generate a signed JWT token using the username as the subject
        //unique
        // embedded in the email link
        String token = jwtUtils.generateToken(username);
        user.setVerificationToken(token);


        userRepo.save(user);

        // try to send Email
        // used  try/catch because SMTP failures throw checked exceptions- tell me when trying to run
        try {
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            //500 Internal Server Error with the reason
            return ResponseEntity.status(500).body("Error sending email " + e.getMessage());
        }

        return ResponseEntity.ok("Registration successful. Check email to verify.");
    }



    //Called when the user clicks the verification link in their email.
    // Validates the JWT token, finds the matching user, and activates their account
    // returns HTML string directly for visual feedback in the browser
    //EMAIL VERIFICATION
    @GetMapping("/verify")
    public String verifyUser(@RequestParam("code") String token) {
        //Validate that the token hasn't been tampered with or expired.
        if (!jwtUtils.validateToken(token)) {
            return "<html><body><h1>Error</h1><p>Invalid or expired token.</p></body></html>";
        }

        //returns null if no match is found
        User user = userRepo.findByVerificationToken(token);


        // guard agaist race condition
        if (user == null) {
            return "<html><body><h1>Error</h1><p>User not found.</p></body></html>";
        }

        //if the account is already active, don't process again, if user clicks link twice
        if (user.isEnabled()) {
            return "<html><body><h1>Already Verified</h1></body></html>";
        }

        // ACTIVATE ACCOUNT clear the token and enable the user.
        user.setVerificationToken(null);
        user.setEnabled(true);
        //UPDATE via JPA
        userRepo.save(user);

        return "<html><body><h1>Success!</h1><p>Account verified.</p></body></html>";
    }

    //LOGIN
    //gives a JWT  token the frontend can use for authenticated requests (stored in localStorage)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        //.orElse(null) unwraps the Optional — returns null if no user found,
        // dont need to  call .isPresent() + .get() separately.
        User user = userRepo.findByUsername(username).orElse(null);
        // check if user is in database and compares the password from the user with the BCrypt hash in the db
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {

            // check if verified
            if (!user.isEnabled()) {
                return ResponseEntity.status(403).body("Account not verified. Check email.");
            }

            // GENERATE THE session token for the next 10 hours -configured in jwtUtils
            // this token is passed in auorization header
            String token = jwtUtils.generateToken(user.getUsername());// so frontend can get username

            // return a JSON object (Map) containing the token.
            // frontend stores this in localStorage
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername()
            ));
        }
        //the username doesn't exist or the password doesn't match
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}