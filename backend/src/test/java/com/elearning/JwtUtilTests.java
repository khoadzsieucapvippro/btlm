package com.elearning;

import com.elearning.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTests {

    private static final String TEST_SECRET = "unit-test-jwt-secret-key-that-is-at-least-32-bytes-long-123456789";
    private static final String OTHER_SECRET = "different-unit-test-jwt-secret-key-at-least-32-bytes-long-9876543";
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
            assertThat(jwtUtil.extractIssuer(token)).isEqualTo("elearning-backend");
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
            assertThat(jwtUtil.extractIssuer(token)).isEqualTo("elearning-backend");
        }

        @Test
        @DisplayName("GIVEN null roles list WHEN generateToken THEN extracts empty roles list without error")
        void testNullRolesHandledSafely() {
            String token = jwtUtil.generateToken("user@elearning.com", null);

            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.extractRoles(token)).isEmpty();
            assertThat(jwtUtil.extractIssuer(token)).isEqualTo("elearning-backend");
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
    @DisplayName("JWT Issuer (iss) Validation Tests (BE-AUTH-003)")
    class IssuerValidationTests {

        @Test
        @DisplayName("GIVEN token with correct expected issuer WHEN validated THEN returns true and extracts correct issuer")
        void testTokenWithCorrectIssuerAccepted() {
            String token = jwtUtil.generateToken("learner@elearning.com", List.of("Learner"));

            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.extractIssuer(token)).isEqualTo("elearning-backend");
            assertThat(jwtUtil.getIssuer()).isEqualTo("elearning-backend");
        }

        @Test
        @DisplayName("GIVEN token with wrong/foreign issuer signed with identical key WHEN validated THEN returns false and throws IncorrectClaimException on extraction")
        void testTokenWithWrongIssuerRejected() {
            // Token signed with identical key but having different issuer
            JwtUtil rogueJwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS, "rogue-issuer-auth-service");
            String rogueToken = rogueJwtUtil.generateToken("victim@elearning.com", List.of("Admin"));

            // Must be rejected by the standard application JwtUtil expecting "elearning-backend"
            assertThat(jwtUtil.validateToken(rogueToken)).isFalse();

            assertThatThrownBy(() -> jwtUtil.extractAllClaims(rogueToken))
                    .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class)
                    .hasMessageContaining("iss");
        }

        @Test
        @DisplayName("GIVEN token with missing issuer claim (no iss) signed with identical key WHEN validated THEN returns false and throws MissingClaimException on extraction")
        void testTokenWithMissingIssuerRejected() {
            // Manually construct token signed with identical secret key but omitting .issuer()
            String tokenWithoutIssuer = io.jsonwebtoken.Jwts.builder()
                    .subject("legacy-user@elearning.com")
                    .claim("roles", List.of("Learner"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000L))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .compact();

            // Must be rejected because iss is mandatory (BE-AUTH-003)
            assertThat(jwtUtil.validateToken(tokenWithoutIssuer)).isFalse();

            assertThatThrownBy(() -> jwtUtil.extractAllClaims(tokenWithoutIssuer))
                    .isInstanceOf(io.jsonwebtoken.MissingClaimException.class)
                    .hasMessageContaining("iss");
        }

        @Test
        @DisplayName("GIVEN token created for Environment A WHEN validated by validator configured for Environment B THEN returns false (Cross-Environment Rejection)")
        void testCrossEnvironmentIssuerRejection() {
            JwtUtil envAJwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS, "elearning-staging");
            JwtUtil envBJwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS, "elearning-production");

            String stagingToken = envAJwtUtil.generateToken("user@elearning.com", List.of("Learner"));

            // Staging token validates in Staging
            assertThat(envAJwtUtil.validateToken(stagingToken)).isTrue();

            // Staging token REJECTED in Production
            assertThat(envBJwtUtil.validateToken(stagingToken)).isFalse();
            assertThatThrownBy(() -> envBJwtUtil.extractAllClaims(stagingToken))
                    .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
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
            String expiredToken = jwtUtil.generateToken(subject, List.of("Learner"), 1L, -5000L);

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
    @DisplayName("Configuration & Key/Issuer Safety")
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

        @Test
        @DisplayName("GIVEN null or blank issuer WHEN constructed THEN throws IllegalArgumentException")
        void testNullOrBlankIssuerRejected() {
            assertThatThrownBy(() -> new JwtUtil(TEST_SECRET, EXPIRATION_MS, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new JwtUtil(TEST_SECRET, EXPIRATION_MS, ""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new JwtUtil(TEST_SECRET, EXPIRATION_MS, "   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("GIVEN Spring Context without jwt.secret WHEN starting THEN fails fast on unresolvable placeholder")
        void testSpringContextFailsFastWhenSecretMissing() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                    .withUserConfiguration(JwtUtil.class)
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                                .getRootCause().hasMessageContaining("jwt.secret");
                    });
        }

        @Test
        @DisplayName("GIVEN Spring Context with blank jwt.secret WHEN starting THEN fails fast in JwtUtil constructor")
        void testSpringContextFailsFastWhenSecretBlank() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                    .withUserConfiguration(JwtUtil.class)
                    .withPropertyValues("jwt.secret=   ")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(IllegalArgumentException.class);
                    });
        }

        @Test
        @DisplayName("GIVEN Spring Context with short jwt.secret WHEN starting THEN fails fast in JwtUtil constructor")
        void testSpringContextFailsFastWhenSecretTooShort() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                    .withUserConfiguration(JwtUtil.class)
                    .withPropertyValues("jwt.secret=too-short-secret-123")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                                .getRootCause().hasMessageContaining("at least 32 bytes");
                    });
        }

        @Test
        @DisplayName("GIVEN Spring Context with valid externalized jwt.secret and jwt.issuer WHEN starting THEN initializes JwtUtil bean successfully")
        void testSpringContextInitializesSuccessfullyWithValidSecretAndIssuer() {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                    .withUserConfiguration(JwtUtil.class)
                    .withPropertyValues(
                            "jwt.secret=" + TEST_SECRET,
                            "jwt.issuer=elearning-backend"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(JwtUtil.class);
                        JwtUtil bean = context.getBean(JwtUtil.class);
                        assertThat(bean.getIssuer()).isEqualTo("elearning-backend");
                    });
        }
    }

    @Nested
    @DisplayName("Authorization Version (auth_ver) Tests (BE-AUTH-004)")
    class AuthorizationVersionTests {

        @Test
        @DisplayName("GIVEN token generated with default authorization version WHEN extracted THEN returns 1L")
        void testDefaultAuthVersion() {
            String token = jwtUtil.generateToken("user@elearning.com", List.of("Learner"));

            Long version = jwtUtil.extractAuthorizationVersion(token);
            assertThat(version).isEqualTo(1L);
        }

        @Test
        @DisplayName("GIVEN token generated with explicit authorization version WHEN extracted THEN returns exact version")
        void testExplicitAuthVersion() {
            String token = jwtUtil.generateToken("creator@elearning.com", List.of("Creator"), 5L);

            Long version = jwtUtil.extractAuthorizationVersion(token);
            assertThat(version).isEqualTo(5L);
        }

        @Test
        @DisplayName("GIVEN token without auth_ver claim WHEN extractAuthorizationVersion THEN returns null")
        void testMissingAuthVersionReturnsNull() {
            String legacyToken = io.jsonwebtoken.Jwts.builder()
                    .issuer("elearning-backend")
                    .subject("legacy@elearning.com")
                    .claim("roles", List.of("Learner"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000L))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .compact();

            Long version = jwtUtil.extractAuthorizationVersion(legacyToken);
            assertThat(version).isNull();
        }

        @Test
        @DisplayName("GIVEN custom expiration and explicit authorization version WHEN generated THEN both claims are correct")
        void testCustomExpAndExplicitAuthVersion() {
            String token = jwtUtil.generateToken("admin@elearning.com", List.of("Admin"), 3L, 7200000L);

            Long version = jwtUtil.extractAuthorizationVersion(token);
            assertThat(version).isEqualTo(3L);
            assertThat(jwtUtil.extractRoles(token)).containsExactly("Admin");
        }
    }
}
