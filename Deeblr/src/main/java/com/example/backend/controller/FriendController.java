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

    // 1. SEND FRIEND REQUEST
    @PostMapping("/request/{targetUsername}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String targetUsername, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        String currentUsername = principal.getName();
        User requester = userRepo.findByUsername(currentUsername).orElseThrow();
        User addressee = userRepo.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //ENSURE USER CANNOT ADD THEMSELVES
        if (requester.equals(addressee)) return ResponseEntity.badRequest().body("Cannot add yourself.");

        //check both directions
        //cannot do A->B if B->A already exists.
        if (friendshipRepository.existsConnection(requester, addressee)) {
            return ResponseEntity.badRequest().body("Request already exists.");
        }

        //create relationship
        Friend friendship = new Friend();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendStatus.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());

        friendshipRepository.save(friendship);
        return ResponseEntity.ok(Collections.singletonMap("message", "Request sent!"));
    }

    // ACCEPT REQUEST
    @PostMapping("/accept/{senderUsername}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String senderUsername, Principal principal) {
        String currentUsername = principal.getName();
        User currentUser = userRepo.findByUsername(currentUsername).orElseThrow();
        User requester = userRepo.findByUsername(senderUsername).orElseThrow();

        // find the request where anothe user ased the current user
        Friend friendship = friendshipRepository.findByRequesterAndAddressee(requester, currentUser)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // only accept if its pending
        if (friendship.getStatus() == FriendStatus.PENDING) {
            friendship.setStatus(FriendStatus.ACCEPTED);
            friendshipRepository.save(friendship);
            return ResponseEntity.ok(Collections.singletonMap("message", "Accepted!"));
        }
        return ResponseEntity.badRequest().body("Invalid status");
    }
    // REJECT REQUEST
    @PostMapping("/reject/{senderUsername}")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable String senderUsername, Principal principal) {
        String currentUsername = principal.getName();
        User currentUser = userRepo.findByUsername(currentUsername).orElseThrow();
        User requester = userRepo.findByUsername(senderUsername).orElseThrow();

        // find the pending request
        Friend friendship = friendshipRepository.findByRequesterAndAddressee(requester, currentUser)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // only reject if it is currently pending
        if (friendship.getStatus() == FriendStatus.PENDING) {
            friendshipRepository.delete(friendship); // delete the request
            return ResponseEntity.ok(Collections.singletonMap("message", "Request rejected"));
        }
        return ResponseEntity.badRequest().body("Invalid status");
    }

    // GET PENDING REQUESTS
    @GetMapping("/requests")
    public List<String> getPendingRequests(Principal principal) {
        User currentUser = userRepo.findByUsername(principal.getName()).orElseThrow();

        // Find all rows where adressee is the target AND status is PENDING
        List<Friend> requests = friendshipRepository.findAllByAddresseeAndStatus(currentUser, FriendStatus.PENDING);

        // just extract the username string and push to frontend
        return requests.stream()
                .map(f -> f.getRequester().getUsername())
                .collect(Collectors.toList());
    }
}