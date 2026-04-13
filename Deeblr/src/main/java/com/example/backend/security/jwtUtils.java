package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component // register this class as a Bean so we can @Autowired it elsewhere.
public class jwtUtils {

    // generate a random key in memory.
    //every time the server restarts, this key changes, invalidating all old tokens.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    //86400000 ms = 24 hours
    private final long EXPIRATION_TIME = 86400000; // 24 hours

    //generate
    // - this method builds the "token" we give to the user.
    // - @param email We use the email (or username) as the unique identifier ("Subject").
    // - @return A long String (the JWT)
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email) //Storing the user's ID inside the token payload
                .setIssuedAt(new Date()) //Storing when it was made
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) //Storing when it dies
                .signWith(key) //Digitally signing it so it can't be tampered with
                .compact(); // compressing it into a string
    }

    // methods to help open the token to read whats inside
    // Extract email from the token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //what acc parses the token.
    //THROW AN EXCEPTION if the token is expired or fake - signature doesn't match.
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // check and validates the token signature and expiration
    public boolean validateToken(String token) {
        try {
            //if this line runs without error the token is valid
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            //If any error occurs (ExpiredJwtException, SignatureException), the token is bad.
            return false;
        }
    }
}