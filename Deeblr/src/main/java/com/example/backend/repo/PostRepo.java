package com.example.backend.repo;

import com.example.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepo extends JpaRepository<Post, Long> {
    // helps finds all posts and sorts them so the newest is at the top
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByUsernameIn(List<String> usernames);
}