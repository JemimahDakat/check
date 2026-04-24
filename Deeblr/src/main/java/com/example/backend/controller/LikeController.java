package com.example.backend.controller;

import com.example.backend.entity.Like;
import com.example.backend.security.jwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
        import java.util.Map;

// 1. The Repository (Put this at the top of the file, or in a separate folder)
interface LikeRepository extends JpaRepository<Like, Long> {
    Like findByPostIdAndUsername(Long postId, String username);
    long countByPostId(Long postId);
}

// 2. The Controller
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private jwtUtils jwtUtils;

    // Get total likes for a post
    @GetMapping("/post/{postId}")
    public long getLikes(@PathVariable Long postId) {
        return likeRepository.countByPostId(postId);
    }

    // Toggle a like (Like / Unlike)
    @PostMapping("/post/{postId}")
    @Transactional
    public Map<String, String> toggleLike(@PathVariable Long postId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtils.extractEmail(token); // Or extractUsername() depending on your setup

        Like existingLike = likeRepository.findByPostIdAndUsername(postId, username);

        if (existingLike != null) {
            // User already liked it, so we UNLIKE it
            likeRepository.delete(existingLike);
            return Map.of("status", "unliked");
        } else {
            // User hasn't liked it, so we save a new LIKE
            likeRepository.save(new Like(postId, username));
            return Map.of("status", "liked");
        }
    }
}