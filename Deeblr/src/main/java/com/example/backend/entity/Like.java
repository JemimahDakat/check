package com.example.backend.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "likes")
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private String username;

    // Empty constructor required by Spring
    public Like() {}

    public Like(Long postId, String username) {
        this.postId = postId;
        this.username = username;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public String getUsername() { return username; }
}