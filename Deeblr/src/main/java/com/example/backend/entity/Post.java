package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    //denormalisation.
    // not using manytoone to users table -  store the username as a String.
    // do not need to load the user's full profile to display their name on a post.
    private String username;

    private String content;
    //do not store the actual image file in the database because it would be too slow/heavy.
    // store the URL to where the file is hosted in aws.
    private String mediaUrl;

    //This object  tells the Frontend whether to render an <img /> tag or a <video /> tag.
    //values will be "IMAGE" or "VIDEO".
    private String mediaType; // "IMAGE" or "VIDEO"
    //Defaults to the current time when the object is created.
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "is_deepfake")
    private Boolean isDeepfake = false;


    //getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsDeepfake() { return isDeepfake; }
    public void setIsDeepfake(Boolean isDeepfake) { this.isDeepfake = isDeepfake; }
}