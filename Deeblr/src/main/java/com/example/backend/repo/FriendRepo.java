package com.example.backend.repo;

import com.example.backend.entity.Friend;
import com.example.backend.entity.User;
import com.example.backend.enumeration.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepo extends JpaRepository<Friend, Long> {

    //checks if a relationship exists -boolean
    boolean existsByRequesterAndAddressee(User requester, User addressee);

    // finds a specific friendship then returns the object
    Optional<Friend> findByRequesterAndAddressee(User requester, User addressee);


    // finds the LIST of friends based on status
    List<Friend> findByAddresseeAndStatus(User addressee, FriendStatus status);
    List<Friend> findByRequesterAndStatus(User requester, FriendStatus status);

    List<Friend> findAllByRequesterAndStatus(User requester, FriendStatus status);
    List<Friend> findAllByAddresseeAndStatus(User addressee, FriendStatus status);
}