package com.example.backend.repo;

import com.example.backend.entity.Friend;
import com.example.backend.entity.User;
import com.example.backend.enumeration.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepo extends JpaRepository<Friend, Long> {

    // This custom query checks if a relationship exists in EITHER direction (A->B or B->A).
    // It returns true if it finds any row, saving us from running two separate queries in Java.
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friend f " +
            "WHERE (f.requester = :user1 AND f.addressee = :user2) " +
            "OR (f.requester = :user2 AND f.addressee = :user1)")
    boolean existsConnection(@Param("user1") User user1, @Param("user2") User user2);

    // FETCH SPECIFIC: Used when Accepting/Declining a specific request.
    Optional<Friend> findByRequesterAndAddressee(User requester, User addressee);

    // FETCH LISTS:
    // 1. "Who has asked me?" (Incoming Requests)
    List<Friend> findAllByAddresseeAndStatus(User addressee, FriendStatus status);

    // 2. "Who have I asked?" (Outgoing Requests)
    // (Consolidated duplicate methods here for cleaner code)
    List<Friend> findAllByRequesterAndStatus(User requester, FriendStatus status);
}