package com.example.backend.controller;

import com.example.backend.entity.Friend;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.enumeration.FriendStatus;
import com.example.backend.repo.FriendRepo;
import com.example.backend.repo.PostRepo;
import com.example.backend.repo.UserRepo;
import com.example.backend.service.S3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired private PostRepo postRepository;
    @Autowired private UserRepo userRepo;
    @Autowired private FriendRepo friendRepo;
    @Autowired private S3 s3Service;

    @GetMapping
    public List<Post> getFeed() {
        String currentName = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(currentName).orElse(null);
        if (me == null) return List.of();

        // get friends lists
        List<Friend> sent = friendRepo.findAllByRequesterAndStatus(me, FriendStatus.ACCEPTED);
        List<Friend> received = friendRepo.findAllByAddresseeAndStatus(me, FriendStatus.ACCEPTED);

        //declare the list
        List<String> friendsUsernames = new ArrayList<>();

        // add yourself and friends
        friendsUsernames.add(currentName);
        sent.forEach(f -> friendsUsernames.add(f.getAddressee().getUsername()));
        received.forEach(f -> friendsUsernames.add(f.getRequester().getUsername()));

        // 4. Fetch posts
        return postRepository.findByUsernameIn(friendsUsernames);
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestParam("file") MultipartFile file,
                                        @RequestParam("content") String content,
                                        @RequestParam("username") String username) throws IOException {
        String url = s3Service.uploadFile(file);

        Post post = new Post();
        post.setUsername(username);
        post.setContent(content);
        post.setMediaUrl(url);
        post.setMediaType(file.getContentType().startsWith("video") ? "VIDEO" : "IMAGE");

        postRepository.save(post);
        return ResponseEntity.ok(post);
    }
}