package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest; //headers, params, body,
import jakarta.servlet.http.HttpServletResponse; //status codes, headers, body
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; //standard object for representing an authenticated user.
// holds  the principal (who), credentials (proof), and authorities (roles)
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; //guarantees doFilterInternal() is called EXACTLY ONCE

import java.io.IOException;
import java.util.Collections;


// the filter intercepts all  HTTP requests coming into the server
// its job is to check if the user sent a valid JWT Token
// check if the request carries a valid JWT in the Authorization header.
//If valid, extract the user's identity and register them with Spring Security.
// ALYWS pass the request onwards, regardless of outcome —
// SecurityConfig Blocks unauthorised access
@Component
public class jwtFilter extends OncePerRequestFilter {

    //used final because it's injected once at construction and should never change.
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