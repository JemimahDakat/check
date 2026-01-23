package com.example.backend.controller;

import com.example.backend.entity.Friend;
import com.example.backend.entity.User;
import com.example.backend.enumeration.FriendStatus;
import com.example.backend.repo.FriendRepo;
import com.example.backend.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired private UserRepo userRepo;
    @Autowired private FriendRepo friendshipRepository;

    // 1. SEND REQUEST (Changed to accept Username String)
    @PostMapping("/request/{targetUsername}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String targetUsername, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        String currentUsername = principal.getName();
        User requester = userRepo.findByUsername(currentUsername).orElseThrow();
        User addressee = userRepo.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requester.equals(addressee)) return ResponseEntity.badRequest().body("Cannot add yourself.");

        boolean exists = friendshipRepository.existsByRequesterAndAddressee(requester, addressee) ||
                friendshipRepository.existsByRequesterAndAddressee(addressee, requester);

        if (exists) return ResponseEntity.badRequest().body("Request already exists.");

        Friend friendship = new Friend();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendStatus.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());

        friendshipRepository.save(friendship);
        return ResponseEntity.ok(Collections.singletonMap("message", "Request sent!"));
    }

    // 2. ACCEPT REQUEST (Changed to accept Username String)
    @PostMapping("/accept/{senderUsername}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String senderUsername, Principal principal) {
        String currentUsername = principal.getName();
        User currentUser = userRepo.findByUsername(currentUsername).orElseThrow();
        User requester = userRepo.findByUsername(senderUsername).orElseThrow();

        // Find the pending request
        Friend friendship = friendshipRepository.findByRequesterAndAddressee(requester, currentUser)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (friendship.getStatus() == FriendStatus.PENDING) {
            friendship.setStatus(FriendStatus.ACCEPTED);
            friendshipRepository.save(friendship);
            return ResponseEntity.ok(Collections.singletonMap("message", "Accepted!"));
        }
        return ResponseEntity.badRequest().body("Invalid status");
    }

    // 3. GET PENDING REQUESTS (Missing piece!)
    @GetMapping("/requests")
    public List<String> getPendingRequests(Principal principal) {
        User currentUser = userRepo.findByUsername(principal.getName()).orElseThrow();

        // Find all rows where I am the addressee AND status is PENDING
        List<Friend> requests = friendshipRepository.findByAddresseeAndStatus(currentUser, FriendStatus.PENDING);

        // Return just the usernames of people asking
        return requests.stream()
                .map(f -> f.getRequester().getUsername())
                .collect(Collectors.toList());
    }
}