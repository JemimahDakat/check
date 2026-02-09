package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

//this class represents a User in our system.
// We use the @Entity annotation to tell Hibernate (our database manager)
// * to automatically create a table in the database called 'users' based on this class.
@Entity
@Table(name = "users")
public class User {

    //@Id marks this field as the Primary Key (the unique identifier for this row).
    //    // @GeneratedValue(strategy = IDENTITY) tells the database to auto-increment the number every time a new user is registered.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //RULE- no two users can have the same username.
    // nullable = false means that this field can never be empty
    @Column(unique = true, nullable = false)
    private String username;

    // store password here
    @Column(nullable = false)
    private String password;

    @Column(unique = true , nullable = false)
    private String email;

    // 0 = Not Verified, 1 = Verified
    private boolean enabled;

    // stores the random string we send to the user's email for verification.
    //We set the length to 512 characters to allow for long, secure tokens.
    @Column(name = "verification_token", length = 512)
    private String verificationToken;

    //the user's permission level. defalut is user but later change to have admin or user controls
    private String role = "USER";

    //'updatable = false' once this date is set, it can never be changed by code.
    // This preserves the integrity of our audit logs.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    //PrePersist is a "Lifecycle Hook".
    //    // It runs automatically right before the data is saved to the database.
    //    // We use it to set default values (like the current time) so we don't forget to do it manually.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // This now handles verification status
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}