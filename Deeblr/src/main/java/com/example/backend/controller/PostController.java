package com.example.backend.controller;

import com.example.backend.entity.Post;
import com.example.backend.enumeration.FriendStatus;
import com.example.backend.repo.PostRepo;
import com.example.backend.repo.UserRepo;
import com.example.backend.service.S3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController //tells spring that this class handles HTTP requests and returns JSON."
@RequestMapping("/api/posts") //All methods here start with /api/posts
public class PostController {

    @Autowired
    private PostRepo postRepository;

    @Autowired
    private S3 s3Service;

    @Autowired
    private UserRepo userRepo;
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
                                        @RequestParam("content") String content,
                                        @RequestParam(value = "isDeepfake", defaultValue = "false") boolean isDeepfake) throws IOException {

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

        // verdict from the frontend before saving
        post.setIsDeepfake(isDeepfake);

        //Return 200 OK with the created post data (so the frontend can display it immediately)
        postRepository.save(post);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/analyse")
    public ResponseEntity<?> analyzseVideoBeforePost(@RequestParam("file") MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Note: Ensure your Python app.py has the route @app.route('/analyse', methods=['POST'])
            String pythonUrl = "http://localhost:5000/analyse";

            // Package the file to send to Python
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.mp4";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call Python and return the result to the Frontend
            Map<String, Object> response = restTemplate.postForObject(pythonUrl, requestEntity, Map.class);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Graceful Degradation: If Python is down, return a JSON map with false.
            // This prevents the frontend JavaScript from crashing when it looks for 'result.is_fake'
            return ResponseEntity.status(500).body(Map.of(
                    "is_fake", false,
                    "confidence", 0.0,
                    "error", "Analysis failed or AI offline: " + e.getMessage()
            ));
        }
    }
}