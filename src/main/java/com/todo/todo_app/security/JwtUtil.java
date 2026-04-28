// JWT Utility

package com.todo.todo_app.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;
    private final long tempTokenExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.temp-token-expiration-ms}") long tempTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.tempTokenExpirationMs = tempTokenExpirationMs;
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    // Temp token for MFA
    public String generateTempToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("mfa_pending", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tempTokenExpirationMs))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract the userId from a valid token
    public Long extractUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.parseLong(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // Returns true only if the token parses and has not expired
    public boolean isTokenValid(String token) {
        return extractUserId(token) != null;
    }

    public boolean isMfaPending(String token) {
        try {
            Boolean mfaPending = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("mfa_pending", Boolean.class);
            return Boolean.TRUE.equals(mfaPending);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
