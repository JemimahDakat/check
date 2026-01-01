package com.example.backend.controller;
import com.example.backend.entity.User;
import com.example.backend.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController //tells spring that this  file handles web requests
@RequestMapping("/api/auth") // to access this link start with /api/auth
@CrossOrigin(origins = "*") // html communicates with java
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register") //// api listens for POST requests at /register
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        //check if user/ email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Email is already in use!");
        }
        //check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Username is taken!");
        }

        // create new user object
        // Note: For now, we save plain text. We will add BCrypt hashing in the next step.
        User newUser = new User(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );

        // save to Database
        userRepository.save(newUser);
        // create a list of pairs
        // This automatically turns into: { "message": "User registered successfully!" }
        // transllates object into JSON so JavaScript doesnt crash
        return ResponseEntity.ok(Collections.singletonMap("message", "User registered successfully!"));
    }
    //login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        //find the user
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 2. Check Password (Plain text for now, match exactly what is in DB)
            if (user.getPassword().equals(password)) {
                // Success: Return user info (excluding password)
                return ResponseEntity.ok(Map.of(
                        "message", "Login successful",
                        "username", user.getUsername(),
                        "id", user.getId()
                ));
            }
        }

        // Failure
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
    }
}