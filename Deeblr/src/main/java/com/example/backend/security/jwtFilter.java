package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;


// the filter intercepts all  HTTP requests coming into the server
// its job is to check if the user sent a valid JWT Token.
@Component
public class jwtFilter extends OncePerRequestFilter {

    private final jwtUtils jwtUtils;

    public jwtFilter(jwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // look for the authorisation header in the incomig request
        String authHeader = request.getHeader("Authorization");

        // check if it exists and starts with bearer
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // extracting the token string exculding BEARER PREFIX

            // validate the token, chick if its expired
            if (jwtUtils.validateToken(token)) {
                // Extract email/ username from token if valid
                String email = jwtUtils.extractEmail(token);

                //create authenication token
                // Tell Spring Securitys the email is verifed
                //  pass 'null' for credentials because the token itself is the proof
                //emptyList) is for Roles (Admin/User) ADD LATER
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

                //Add request details (like IP address) to the authentication object.
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //Set the SecurityContext.
                // tells system, For the duration of this request, this user is officially logged in untill logged out
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Whether a token was found or not, we pass the request to the next step.
        // If the user didn't have a token, and they try to access a private page,
        // the SecurityConfig rules will block them later.
        chain.doFilter(request, response);
    }
}