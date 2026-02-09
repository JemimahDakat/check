package com.example.backend.config;

import com.example.backend.security.jwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration //Tells Spring this class contains configuration settings.
@EnableWebSecurity // Activates Spring Security's web protection features.

public class SecurityConfig {

    private final jwtFilter jwtFilter;

    //custom Jwt filter is avalible here
    public SecurityConfig(jwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery).
                // We disable this because we are using JWT Tokens (Stateless).
                // CSRF protection is generally needed for Session/Cookie-based apps.
                .csrf(csrf -> csrf.disable())

                // CORS (Cross-Origin Resource Sharing) Configuration.
                // Browsers block requests from different ports by default (e.g., 63342 -> 8080).
                // We must explicitly allow the Frontend URL to talk to the Backend.
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    // EXACT URL of  Frontend
                    config.setAllowedOrigins(List.of("http://localhost:63342"));

                    // Allow all methods (GET, POST, OPTIONS, etc.)
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

                    // Allow all headers so we can send the "Authorization" header with our token.
                    config.setAllowedHeaders(List.of("*"));

                    // Allow credentials (like tokens) to be passed back and forth.
                    config.setAllowCredentials(true);

                    return config;
                }))

                // defines which pages are public and which are private.
                .authorizeHttpRequests(auth -> auth
                        // public pages: Login, Register, Homepage, and assets CSS/JS pages
                        .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/feed.html", "/upload.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Private Pages: Everything else requires token/  the user to be logged in.
                        .anyRequest().authenticated()
                )

                //  insert  custom 'jwtFilter' BEFORE the standard 'UsernamePasswordAuthenticationFilter'.
                // makes sure the system checks the Token first. If the token is valid, the user is logged in automatically.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}