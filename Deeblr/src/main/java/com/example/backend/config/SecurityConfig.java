package com.example.backend.config;

import com.example.backend.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Not needed for stateless JWT)
                .csrf(csrf -> csrf.disable())

                // 2. Enable CORS (Allow frontend to talk to backend if needed)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:8080")); // Allow your frontend
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))

                // 3. Make Session STATELESS (No cookies stored on server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Define Access Rules
                .authorizeHttpRequests(auth -> auth
                        // ✅ OPEN: Login/Register API Endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ OPEN: All HTML pages & Static assets (CSS/JS/Images)
                        // We leave 'feed.html' open so the browser can load it,
                        // but the script inside will block access if no token exists.
                        .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/feed.html", "/css/**", "/js/**").permitAll()

                        // 🔒 CLOSED: All other API endpoints (Data)
                        // Requesting /api/posts without a token will fail with 403 Forbidden
                        .requestMatchers("/api/**").authenticated()

                        // Catch-all
                        .anyRequest().authenticated()
                )

                // 5. Add the JWT Filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}