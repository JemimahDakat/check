package com.example.backend.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. turn off complex stamp check CSRF - for using Postman
                .csrf(csrf -> csrf.disable())

                // 2. configure which pages are public vs private
                .authorizeHttpRequests(auth -> auth
                        // allow anyone to visit register and login pages
                        .requestMatchers("/api/auth/**").permitAll()
                        // Lock everything else
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}