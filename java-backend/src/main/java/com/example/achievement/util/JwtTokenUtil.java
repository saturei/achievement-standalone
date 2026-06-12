package com.example.achievement.util;

import com.example.achievement.config.DingTalkProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenUtil {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final String FALLBACK_SECRET = "achievement-standalone-default-jwt-secret-key-2024";

    @Autowired
    private DingTalkProperties dingTalkProperties;

    private SecretKey getSigningKey() {
        try {
            String secret = (dingTalkProperties != null && dingTalkProperties.getAppSecret() != null)
                    ? dingTalkProperties.getAppSecret()
                    : FALLBACK_SECRET;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Generate a JWT token for the given DingTalk userId/unionId.
     *
     * @param userId the DingTalk userId or unionId
     * @return signed JWT string
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("dingtalkUserId", userId);

        String token = Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();

        log.debug("Generated JWT for userId: {}, expires at: {}", userId, expiration);
        return token;
    }

    /**
     * Validate the JWT token and return the userId (subject) if valid.
     *
     * @param token the JWT token string
     * @return userId if valid, null otherwise
     */
    public String validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            log.warn("JWT token has empty subject");
            return null;
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract the userId from the JWT token (without full validation).
     * Returns null if parsing fails.
     *
     * @param token the JWT token string
     * @return userId from claims, or null
     */
    public String getUserIdFromToken(String token) {
        return validateToken(token);
    }
}
