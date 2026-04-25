package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repo.UserRepo;
import com.example.backend.security.jwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jwtUtils jwtUtils;

    @PutMapping("/update")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> updates, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String currentUsername = jwtUtils.extractEmail(token);

        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        User user = userOpt.get();

        // updating privacy
        if (updates.containsKey("isPrivate")) {
            user.setPrivate(Boolean.parseBoolean(updates.get("isPrivate")));
        }

        // update username and chck for duplicates
        if (updates.containsKey("newUsername") && !updates.get("newUsername").isBlank()) {
            String newUsername = updates.get("newUsername");
            if (!newUsername.equals(currentUsername) && userRepository.findByUsername(newUsername).isPresent()) {
                return ResponseEntity.status(400).body("Username already taken.");
            }
            user.setUsername(newUsername);
        }

        // update the password
        if (updates.containsKey("newPassword") && !updates.get("newPassword").isBlank()) {
            user.setPassword(passwordEncoder.encode(updates.get("newPassword")));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Settings updated successfully. Please log in again.");
    }
}
