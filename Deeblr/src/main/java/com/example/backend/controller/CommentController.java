package com.example.backend.controller;
import com.example.backend.entity.Comment;
import com.example.backend.security.jwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
}


@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private jwtUtils jwtUtils;

    // get comments for a specific post
    @GetMapping("/post/{postId}")
    public List<Comment> getComments(@PathVariable Long postId) {
        return commentRepository.findByPostId(postId);
    }

    @PostMapping
    public Comment addComment(
            @RequestBody Comment comment,
            @RequestHeader("Authorization") String authHeader) {

        // strip the "Bearer " prefix from the token
        String token = authHeader.substring(7);

        // decode the token to find the true user
        String trueUsername = jwtUtils.extractEmail(token);

        //force the comment to belong to the true user
        comment.setAuthor(trueUsername);

        // save to the database
        return commentRepository.save(comment);
    }
}
