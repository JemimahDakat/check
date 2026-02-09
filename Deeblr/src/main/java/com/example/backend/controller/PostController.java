package com.example.backend.controller;

import com.example.backend.entity.Post;
import com.example.backend.enumeration.FriendStatus;
import com.example.backend.repo.PostRepo;
import com.example.backend.service.S3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController //tells spring that this class handles HTTP requests and returns JSON."
@RequestMapping("/api/posts") //All methods here start with /api/posts
public class PostController {

    @Autowired
    private PostRepo postRepository;

    @Autowired
    private S3 s3Service;

    //GET FEED
    // GET /api/posts
    @GetMapping
    public List<Post> getFeed() {

        //  ask the SecurityContext who the user is not the frontend
        // The 'jwtFilter' has already verified the token and stored the valid username here.
        // the request would never even reach this if the token was invalid
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        //We delegate the complex logic (finding friends, sorting dates) to the Database (PostRepo).
        // This keeps the Controller clean and the application fast.
        // This effectively does "Find User -> Find Friends -> Get Posts" in one step.
        return postRepository.findFeed(currentUser, FriendStatus.ACCEPTED);
    }

    // CREATE POST
    //Endpoint: POST /api/posts
    // this is a Multipart/form-data (File + Text)
    @PostMapping
    public ResponseEntity<?> createPost(@RequestParam("file") MultipartFile file,
                                        @RequestParam("content") String content) throws IOException {

        // Use the Token to verify  the user again is who they say they are
        String realUser = SecurityContextHolder.getContext().getAuthentication().getName();

        //Upload to AWS
        // done before saving to the DB. If upload fails, the code stops HERE
        // so the db does not have missing values.
        String url = s3Service.uploadFile(file);

        // make and save the post
        Post post = new Post();
        post.setUsername(realUser);
        post.setContent(content);
        post.setMediaUrl(url);
        //determine media type based on the file's mime type (e.g., "video/mp4")
        post.setMediaType(file.getContentType().startsWith("video") ? "VIDEO" : "IMAGE");

        //Return 200 OK with the created post data (so the frontend can display it immediately)
        postRepository.save(post);
        return ResponseEntity.ok(post);
    }
}