package com.example.backend.service;
import com.example.backend.entity.User;
import com.example.backend.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class userservicedetailsservice implements UserDetailsService {

    @Autowired
    private UserRepo userRepository;

    //sping calls automatically when it needs to verify a user
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //check the database using our repo
        //used .orElseThrow() because optional forces us to handle the "User Not Found" scenario explicitly.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convert to Spring Security's format.
        //build a standard org.springframework.security.core.userdetails.User object.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // comparees the passworf with what the user typed.
                .authorities(Collections.emptyList()) // for now send an empty list untill add new roles
                .build();
    }
}
