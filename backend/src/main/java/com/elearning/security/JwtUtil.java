package com.elearning.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Enforces mandatory issuer (iss) binding and validation (BE-AUTH-003).
 * Thread-safe and stateless: holds no per-request state.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    public static final String DEFAULT_ISSUER = "elearning-backend";
    public static final String CLAIM_AUTH_VER = "auth_ver";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;
    private final JwtParser jwtParser;

    public JwtUtil(String secret, long expirationMs) {
        this(secret, expirationMs, DEFAULT_ISSUER);
    }

    @Autowired
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${jwt.issuer:elearning-backend}") String issuer) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret cannot be null or blank");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits). Provided length: " + keyBytes.length);
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer cannot be null or blank");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer.trim();
        this.jwtParser = Jwts.parser()
                .requireIssuer(this.issuer)
                .verifyWith(this.signingKey)
                .build();
    }

    /**
     * Gets the configured expected issuer string.
     *
     * @return issuer string
     */
    public String getIssuer() {
        return this.issuer;
    }

    /**
     * Generates a signed JWT with default authorization version (1L) and expiration time.
     *
     * @param subject email or phone identifying the account
     * @param roles   list of roles assigned to the user
     * @return signed JWT string
     */
    public String generateToken(String subject, List<String> roles) {
        return generateToken(subject, roles, 1L, this.expirationMs);
    }

    /**
     * Generates a signed JWT with an explicit authorization version and default expiration time.
     *
     * @param subject              email or phone identifying the account
     * @param roles                list of roles assigned to the user
     * @param authorizationVersion current authorization version of the account
     * @return signed JWT string
     */
    public String generateToken(String subject, List<String> roles, Long authorizationVersion) {
        return generateToken(subject, roles, authorizationVersion, this.expirationMs);
    }

    /**
     * Generates a signed JWT with explicit authorization version, custom expiration duration, and issuer.
     *
     * @param subject              email or phone identifying the account
     * @param roles                list of roles assigned to the user
     * @param authorizationVersion current authorization version of the account
     * @param customExpMs          expiration duration in milliseconds
     * @return signed JWT string
     */
    public String generateToken(String subject, List<String> roles, Long authorizationVersion, long customExpMs) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject cannot be null or blank");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpMs);

        var builder = Jwts.builder()
                .issuer(this.issuer)
                .subject(subject)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.signingKey);

        if (authorizationVersion != null) {
            builder.claim(CLAIM_AUTH_VER, authorizationVersion);
        }

        return builder.compact();
    }

    /**
     * Extracts the issuer (iss) from the token.
     *
     * @param token JWT token string
     * @return issuer string
     */
    public String extractIssuer(String token) {
        return extractAllClaims(token).getIssuer();
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
     * Extracts the authorization version from the token string.
     *
     * @param token JWT token string
     * @return authorization version as Long, or null if claim is missing or invalid
     */
    public Long extractAuthorizationVersion(String token) {
        Claims claims = extractAllClaims(token);
        return extractAuthorizationVersion(claims);
    }

    /**
     * Extracts the authorization version from parsed Claims.
     *
     * @param claims Claims object
     * @return authorization version as Long, or null if claim is missing or invalid
     */
    public Long extractAuthorizationVersion(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object verObj = claims.get(CLAIM_AUTH_VER);
        if (verObj instanceof Number number) {
            return number.longValue();
        }
        if (verObj instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
     * Enforces signature verification and required issuer claim match.
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
     * has not expired, contains the expected issuer, and contains a non-blank subject.
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
