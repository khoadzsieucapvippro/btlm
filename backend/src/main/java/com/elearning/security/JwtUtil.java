package com.elearning.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Utility component for generating, parsing, and validating JSON Web Tokens (JWT).
 * Uses modern JJWT API (0.12.x) with HMAC-SHA256 algorithm.
 * Thread-safe and stateless: holds no per-request state.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey signingKey;
    private final long expirationMs;
    private final JwtParser jwtParser;

    public JwtUtil(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret cannot be null or blank");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits). Provided length: " + keyBytes.length);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.jwtParser = Jwts.parser().verifyWith(this.signingKey).build();
    }

    /**
     * Generates a signed JWT with the default configured expiration time.
     *
     * @param subject email or phone identifying the account
     * @param roles   list of roles assigned to the user
     * @return signed JWT string
     */
    public String generateToken(String subject, List<String> roles) {
        return generateToken(subject, roles, this.expirationMs);
    }

    /**
     * Generates a signed JWT with a custom expiration duration (in milliseconds).
     *
     * @param subject      email or phone identifying the account
     * @param roles        list of roles assigned to the user
     * @param customExpMs  expiration duration in milliseconds
     * @return signed JWT string
     */
    public String generateToken(String subject, List<String> roles, long customExpMs) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject cannot be null or blank");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpMs);

        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.signingKey)
                .compact();
    }

    /**
     * Extracts the subject (email or phone) from the token.
     *
     * @param token JWT token string
     * @return subject string
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the list of roles from the token.
     *
     * @param token JWT token string
     * @return list of role names
     */
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    /**
     * Extracts expiration timestamp from the token.
     *
     * @param token JWT token string
     * @return expiration Date
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Extracts issued-at timestamp from the token.
     *
     * @param token JWT token string
     * @return issued-at Date
     */
    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    /**
     * Parses and returns all claims from a signed JWT.
     * Throws JwtException or IllegalArgumentException if invalid.
     *
     * @param token JWT token string
     * @return Claims payload
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        return this.jwtParser.parseSignedClaims(token).getPayload();
    }

    /**
     * Validates whether a token is structurally valid, has valid signature,
     * has not expired, and contains a non-blank subject.
     *
     * @param token JWT token string
     * @return true if valid; false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject() != null && !claims.getSubject().isBlank();
        } catch (JwtException e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("JWT token argument invalid: {}", e.getMessage());
            return false;
        }
    }
}
