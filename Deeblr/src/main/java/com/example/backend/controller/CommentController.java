package com.example.backend.controller;
import com.example.backend.entity.Comment;
import com.example.backend.security.jwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CommentRepository extends JpaRepository<Comment, Long> {}

// 2. The API Endpoints
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private jwtUtils jwtUtils;

    // Get all comments to display on the page
    @GetMapping
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    @PostMapping
    public Comment addComment(
            @RequestBody Comment comment,
            @RequestHeader("Authorisation") String authHeader) {

        // 1. Strip the "Bearer " prefix from the token
        String token = authHeader.substring(7);

        // 2. Mathematically decode the token to find the true user
        String trueUsername = jwtUtils.extractEmail(token);

        // 3. Force the comment to belong to the true user, ignoring anything the frontend sent
        comment.setAuthor(trueUsername);

        // 4. Save to the database
        return commentRepository.save(comment);
    }
}
