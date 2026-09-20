package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive Verification and Security Tests for BE-AUTH-003:
 * Require and Validate JWT Issuer (iss) Claim.
 *
 * Verifies that:
 * 1. An unexpired JWT signed with the application's secret key and correct issuer ('elearning-backend')
 *    authenticates successfully (200 OK).
 * 2. An unexpired JWT signed with the application's EXACT SAME secret key but a WRONG issuer
 *    ('rogue-service', 'attacker.example') is rejected with HTTP 401 Unauthorized.
 * 3. An unexpired JWT signed with the application's EXACT SAME secret key but MISSING the 'iss' claim
 *    is rejected with HTTP 401 Unauthorized.
 * 4. Authentication fails BEFORE RBAC: A moderator token with a wrong/missing issuer receives
 *    401 Unauthorized, not 403 Forbidden.
 * 5. Account status enforcement (BE-AUTH-001) is preserved: A disabled account with a valid issuer
 *    token is still rejected with 401 Unauthorized.
 * 6. Public endpoints remain accessible unauthenticated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BE-AUTH-003: JWT Issuer (iss) Validation Integration Tests")
class JwtIssuerSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String testSecret;

    @Value("${jwt.issuer:elearning-backend}")
    private String configuredIssuer;

    private Account createAccountWithProfile(String email, String roleName, String status) {
        Account account = new Account();
        account.setEmailOrPhone(email);
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setStatus(status);
        if (roleName != null) {
            roleRepository.findByRoleName(roleName).ifPresent(account::addRole);
        }
        Account savedAccount = accountRepository.saveAndFlush(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(savedAccount);
        profile.setFullName("Test User " + email);
        userProfileRepository.saveAndFlush(profile);

        return savedAccount;
    }

    @Nested
    @DisplayName("1. Core Issuer Validation Tests (Correct vs Wrong vs Missing Issuer)")
    class CoreIssuerTests {

        @Test
        @DisplayName("GIVEN active user with token having CORRECT issuer ('elearning-backend') WHEN accessing protected endpoint THEN 200 OK")
        void testValidTokenWithCorrectIssuer_succeeds() throws Exception {
            String email = "issuer_user_valid@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            // Normal application token generation embeds the configured issuer
            String validToken = jwtUtil.generateToken(email, List.of("Learner"));

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("GIVEN active user with token having WRONG issuer signed with valid key WHEN accessing protected endpoint THEN 401 UNAUTHORIZED (BE-AUTH-003)")
        void testTokenWithWrongIssuer_rejectedWith401() throws Exception {
            String email = "issuer_user_wrong@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            // Create token signed with EXACT SAME secret key, but specifying a different issuer
            JwtUtil foreignJwtUtil = new JwtUtil(testSecret, 3600000L, "foreign-external-auth-server");
            String foreignIssuerToken = foreignJwtUtil.generateToken(email, List.of("Learner"));

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignIssuerToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Chưa xác thực hoặc phiên đăng nhập đã hết hạn"));
        }

        @Test
        @DisplayName("GIVEN active user with token MISSING issuer claim signed with valid key WHEN accessing protected endpoint THEN 401 UNAUTHORIZED (BE-AUTH-003)")
        void testTokenWithMissingIssuer_rejectedWith401() throws Exception {
            String email = "issuer_user_missing@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            // Construct token signed with EXACT SAME secret key, but without any 'iss' claim
            String tokenWithoutIssuer = Jwts.builder()
                    .subject(email)
                    .claim("roles", List.of("Learner"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000L))
                    .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutIssuer))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Chưa xác thực hoặc phiên đăng nhập đã hết hạn"));
        }
    }

    @Nested
    @DisplayName("2. Role-Protected Endpoints & RBAC Precedence Tests")
    class RoleIssuerTests {

        @Test
        @DisplayName("GIVEN Moderator token with WRONG issuer WHEN accessing Moderator endpoint THEN 401 UNAUTHORIZED (Auth fails before RBAC)")
        void testModeratorTokenWithWrongIssuer_failsAuthBeforeRbac() throws Exception {
            String email = "mod_wrong_iss@example.com";
            createAccountWithProfile(email, "Moderator", "Active");

            JwtUtil foreignJwtUtil = new JwtUtil(testSecret, 3600000L, "untrusted-issuer");
            String modTokenWithWrongIssuer = foreignJwtUtil.generateToken(email, List.of("Moderator"));

            // Must return 401 Unauthorized, NOT 403 Forbidden
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + modTokenWithWrongIssuer))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Creator token with MISSING issuer WHEN accessing Creator endpoint THEN 401 UNAUTHORIZED")
        void testCreatorTokenWithMissingIssuer_failsAuth() throws Exception {
            String email = "creator_missing_iss@example.com";
            createAccountWithProfile(email, "Creator", "Active");

            String creatorTokenWithoutIssuer = Jwts.builder()
                    .subject(email)
                    .claim("roles", List.of("Creator"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000L))
                    .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorTokenWithoutIssuer))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("3. Account Status & Public Endpoints Interaction")
    class StatusAndPublicTests {

        @Test
        @DisplayName("GIVEN disabled account with CORRECT issuer token WHEN accessing protected endpoint THEN 401 UNAUTHORIZED (BE-AUTH-001 preserved)")
        void testDisabledAccountWithCorrectIssuer_stillRejected() throws Exception {
            String email = "disabled_correct_iss@example.com";
            createAccountWithProfile(email, "Learner", "Inactive");

            String token = jwtUtil.generateToken(email, List.of("Learner"));

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN unauthenticated request to public catalog endpoint WHEN executed THEN 200 OK (Public access unaffected)")
        void testPublicEndpoint_remainsAccessible() throws Exception {
            mockMvc.perform(get("/api/v1/radicals?page=0&size=5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }
}
