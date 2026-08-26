package com.elearning;

import com.elearning.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTests {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String OTHER_SECRET = "8x/A?D(G+KbPeShVmYq3t6w9z$B&E)H@McQfTjWnZr4u7x!A%D*F-JaNdRgUkXp2";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS);
    }

    @Nested
    @DisplayName("Token Generation and Claims Extraction")
    class GenerationAndExtractionTests {

        @Test
        @DisplayName("GIVEN valid subject and roles WHEN generateToken THEN returns valid signed JWT with correct claims")
        void testGenerateAndExtractClaims() {
            String subject = "learner@elearning.com";
            List<String> roles = List.of("Learner");

            String token = jwtUtil.generateToken(subject, roles);

            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.extractSubject(token)).isEqualTo(subject);
            assertThat(jwtUtil.extractRoles(token)).containsExactly("Learner");

            Date issuedAt = jwtUtil.extractIssuedAt(token);
            Date expiration = jwtUtil.extractExpiration(token);

            assertThat(issuedAt).isNotNull().isBeforeOrEqualTo(new Date());
            assertThat(expiration).isNotNull().isAfter(new Date());
            assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(EXPIRATION_MS);
        }

        @Test
        @DisplayName("GIVEN multiple roles WHEN generateToken THEN extracts all roles correctly")
        void testMultipleRoles() {
            String subject = "admin@elearning.com";
            List<String> roles = List.of("Admin", "Moderator", "Creator");

            String token = jwtUtil.generateToken(subject, roles);

            assertThat(jwtUtil.extractRoles(token)).containsExactly("Admin", "Moderator", "Creator");
        }

        @Test
        @DisplayName("GIVEN null roles list WHEN generateToken THEN extracts empty roles list without error")
        void testNullRolesHandledSafely() {
            String token = jwtUtil.generateToken("user@elearning.com", null);

            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.extractRoles(token)).isEmpty();
        }

        @Test
        @DisplayName("GIVEN blank or null subject WHEN generateToken THEN throws IllegalArgumentException")
        void testBlankSubjectThrows() {
            assertThatThrownBy(() -> jwtUtil.generateToken("", List.of("Learner")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> jwtUtil.generateToken("   ", List.of("Learner")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> jwtUtil.generateToken(null, List.of("Learner")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Token Expiration and Invalidation")
    class ExpirationTests {

        @Test
        @DisplayName("GIVEN token generated with past expiration WHEN validated THEN returns false and throws ExpiredJwtException on extraction")
        void testExpiredToken() {
            String subject = "expired@elearning.com";
            // custom expiration in past: -5000 ms
            String expiredToken = jwtUtil.generateToken(subject, List.of("Learner"), -5000L);

            assertThat(jwtUtil.validateToken(expiredToken)).isFalse();

            assertThatThrownBy(() -> jwtUtil.extractAllClaims(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Cryptographic Signature Verification")
    class SignatureTests {

        @Test
        @DisplayName("GIVEN token signed with different secret key WHEN validated THEN returns false and throws JwtException on extraction")
        void testWrongSignature() {
            JwtUtil otherJwtUtil = new JwtUtil(OTHER_SECRET, EXPIRATION_MS);
            String foreignToken = otherJwtUtil.generateToken("alien@elearning.com", List.of("Admin"));

            assertThat(jwtUtil.validateToken(foreignToken)).isFalse();

            assertThatThrownBy(() -> jwtUtil.extractAllClaims(foreignToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("GIVEN tampered token payload WHEN validated THEN returns false")
        void testTamperedPayload() {
            String token = jwtUtil.generateToken("honest@elearning.com", List.of("Learner"));
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);

            // Tamper payload by altering characters
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            String tamperedPayloadJson = payload.replace("honest", "hacked");
            String tamperedPayloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayloadJson.getBytes());

            String tamperedToken = parts[0] + "." + tamperedPayloadBase64 + "." + parts[2];

            assertThat(jwtUtil.validateToken(tamperedToken)).isFalse();
            assertThatThrownBy(() -> jwtUtil.extractAllClaims(tamperedToken))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("Malformed & Illegal Input Tests")
    class MalformedTests {

        @Test
        @DisplayName("GIVEN malformed strings or empty values WHEN validated THEN returns false without crashing")
        void testMalformedInputs() {
            assertThat(jwtUtil.validateToken("")).isFalse();
            assertThat(jwtUtil.validateToken("   ")).isFalse();
            assertThat(jwtUtil.validateToken(null)).isFalse();
            assertThat(jwtUtil.validateToken("not.a.valid.jwt")).isFalse();
            assertThat(jwtUtil.validateToken("random-string-without-dots")).isFalse();
            assertThat(jwtUtil.validateToken("header.payload")).isFalse();
        }

        @Test
        @DisplayName("GIVEN null or blank token WHEN extracting claims THEN throws IllegalArgumentException")
        void testExtractAllClaimsIllegalArg() {
            assertThatThrownBy(() -> jwtUtil.extractAllClaims(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> jwtUtil.extractAllClaims(""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> jwtUtil.extractAllClaims("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Configuration & Key Length Safety")
    class KeySafetyTests {

        @Test
        @DisplayName("GIVEN secret key less than 32 bytes WHEN constructed THEN throws IllegalArgumentException")
        void testShortSecretKeyRejected() {
            String shortSecret = "too-short-secret-key-123"; // 24 bytes

            assertThatThrownBy(() -> new JwtUtil(shortSecret, EXPIRATION_MS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 32 bytes");
        }

        @Test
        @DisplayName("GIVEN null or blank secret WHEN constructed THEN throws IllegalArgumentException")
        void testNullOrBlankSecretRejected() {
            assertThatThrownBy(() -> new JwtUtil(null, EXPIRATION_MS))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new JwtUtil("", EXPIRATION_MS))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new JwtUtil("   ", EXPIRATION_MS))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
