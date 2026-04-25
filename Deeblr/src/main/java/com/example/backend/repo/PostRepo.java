package com.example.backend.repo;

import com.example.backend.entity.Post;
import com.example.backend.enumeration.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
//This interface handles all database operations for Posts.
// *By extending JpaRepository, can  get methods like save(), delete(), and findAll() .
public interface PostRepo extends JpaRepository<Post, Long> {
    // Derived Query: Spring automatically generates SQL to find posts where the username
    // matches one of the names in the provided list.
    List<Post> findByUsernameIn(List<String> usernames);

    @Query("SELECT p FROM Post p WHERE p.username = :me " +
            "OR p.username IN (SELECT f.addressee.username FROM Friend f WHERE f.requester.username = :me AND f.status = :status) " +
            "OR p.username IN (SELECT f.requester.username FROM Friend f WHERE f.addressee.username = :me AND f.status = :status) " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFeed(@Param("me") String myUsername, @Param("status") FriendStatus status);
    @Query(value = "SELECT p.* FROM posts p JOIN users u ON p.username = u.username WHERE u.is_private = false ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Post> findRandomPublicPosts();
}
