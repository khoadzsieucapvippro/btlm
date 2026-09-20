package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive Verification and Security Integration Tests for BE-AUTH-004:
 * Immediate JWT Role Revocation via Authorization Versioning.
 *
 * Verifies that:
 * 1. Tokens matching the current authorization version authenticate successfully.
 * 2. Role addition increments authorization version and immediately invalidates old JWTs (HTTP 401).
 * 3. Role removal increments authorization version and immediately invalidates old JWTs (HTTP 401).
 * 4. Role replacement increments authorization version and immediately invalidates old JWTs (HTTP 401).
 * 5. Fresh login after role change generates a valid token with current version and grants access.
 * 6. Account status verification (Active/Inactive/Banned) remains strictly enforced alongside version checks.
 * 7. Tampering with JWT auth_ver claim fails cryptographic signature validation.
 * 8. Transaction rollback preserves previous authorization version and previous role state atomically.
 * 9. Missing auth_ver claim on authenticated request is rejected with 401.
 * 10. Role downgrade bypass is completely blocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BE-AUTH-004: Immediate JWT Role Revocation Integration Tests")
class RoleRevocationSecurityIntegrationTests {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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
        profile.setFullName("User " + email);
        userProfileRepository.saveAndFlush(profile);

        return savedAccount;
    }

    @Nested
    @DisplayName("1. Same Version & Normal Operation")
    class SameVersionTests {

        @Test
        @DisplayName("GIVEN active account WHEN token contains matching authorization version THEN authenticates with 200 OK")
        void test1_matchingVersionAuthenticatesSuccessfully() throws Exception {
            String email = "matching_ver@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");

            String token = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("2. Role Mutations & Immediate Invalidation")
    class RoleMutationInvalidationTests {

        @Test
        @DisplayName("GIVEN active account WHEN role added and version incremented THEN old token is rejected with 401 (Test 2)")
        void test2_roleAdditionInvalidatesOldToken() throws Exception {
            // 1. Arrange: Create account with Learner role and issue token at version 1
            String email = "role_add_user@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");
            assertThat(account.getAuthorizationVersion()).isEqualTo(1L);

            String oldToken = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            // Baseline: old token works
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isOk());

            // 2. Act: Admin adds Creator role and increments authorization version
            Role creatorRole = roleRepository.findByRoleName("Creator").orElseThrow();
            account.addRole(creatorRole);
            account.incrementAuthorizationVersion();
            accountRepository.saveAndFlush(account);

            assertThat(account.getAuthorizationVersion()).isEqualTo(2L);

            // 3. Assert: Request with old token is rejected with 401 Unauthorized
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Chưa xác thực hoặc phiên đăng nhập đã hết hạn"));
        }

        @Test
        @DisplayName("GIVEN Creator account WHEN role removed and version incremented THEN old token is rejected with 401 (Test 3)")
        void test3_roleRemovalInvalidatesOldToken() throws Exception {
            // 1. Arrange: Create account with Creator role and issue token at version 1
            String email = "role_remove_user@example.com";
            Account account = createAccountWithProfile(email, "Creator", "Active");

            String oldCreatorToken = jwtUtil.generateToken(email, List.of("Creator"), account.getAuthorizationVersion());

            // Baseline: old Creator token accesses creator endpoints
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldCreatorToken))
                    .andExpect(status().isOk());

            // 2. Act: Admin removes Creator role and increments authorization version
            Role creatorRole = roleRepository.findByRoleName("Creator").orElseThrow();
            account.removeRole(creatorRole);
            account.incrementAuthorizationVersion();
            accountRepository.saveAndFlush(account);

            assertThat(account.getAuthorizationVersion()).isEqualTo(2L);

            // 3. Assert: Old creator token is rejected at auth boundary with 401 Unauthorized
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldCreatorToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN account WHEN roles replaced and version incremented THEN old token is rejected with 401 (Test 4)")
        void test4_roleReplacementInvalidatesOldToken() throws Exception {
            // 1. Arrange: Account with Moderator role
            String email = "role_replace_user@example.com";
            Account account = createAccountWithProfile(email, "Moderator", "Active");

            String oldModToken = jwtUtil.generateToken(email, List.of("Moderator"), account.getAuthorizationVersion());

            // Baseline: Old token accesses moderator pending queue
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldModToken))
                    .andExpect(status().isOk());

            // 2. Act: Admin replaces roles with Learner only
            Role learnerRole = roleRepository.findByRoleName("Learner").orElseThrow();
            account.setRoles(new HashSet<>(Set.of(learnerRole)));
            account.incrementAuthorizationVersion();
            accountRepository.saveAndFlush(account);

            assertThat(account.getAuthorizationVersion()).isEqualTo(2L);

            // 3. Assert: Old token is immediately rejected with 401
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldModToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN role change invalidates old token WHEN fresh token issued THEN fresh token succeeds (Test 5)")
        void test5_freshTokenAfterRoleChangeSucceeds() throws Exception {
            String email = "fresh_token_user@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");

            String oldToken = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            // Promote to Creator
            Role creatorRole = roleRepository.findByRoleName("Creator").orElseThrow();
            account.addRole(creatorRole);
            account.incrementAuthorizationVersion();
            Account updatedAccount = accountRepository.saveAndFlush(account);

            // Old token rejected
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized());

            // Fresh token with new version 2L and Creator role succeeds
            String freshToken = jwtUtil.generateToken(email, List.of("Learner", "Creator"), updatedAccount.getAuthorizationVersion());

            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + freshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("3. Security Invariants & Downgrade Prevention")
    class SecurityInvariantsTests {

        @Test
        @DisplayName("GIVEN matching version BUT Banned/Inactive status WHEN request made THEN rejected with 401 (Test 6)")
        void test6_bannedAccountRejectedRegardlessOfVersion() throws Exception {
            String email = "banned_ver_user@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Banned");

            String token = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN token with tampered auth_ver claim WHEN presented THEN fails cryptographic signature with 401 (Test 7)")
        void test7_tamperedAuthVersionFailsSignature() throws Exception {
            String email = "tamper_user@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            // Build token signed with a rogue key pretending to have auth_ver 2L
            String rogueSecret = "rogue-secret-key-that-is-at-least-32-bytes-long-1234567";
            JwtUtil rogueJwt = new JwtUtil(rogueSecret, 3600000L);
            String rogueToken = rogueJwt.generateToken(email, List.of("Learner"), 2L);

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + rogueToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN token with missing auth_ver claim WHEN presented THEN rejected with 401 Unauthorized (Test 9)")
        void test9_missingAuthVersionClaimRejected() throws Exception {
            String email = "missing_claim_user@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            // Build token without auth_ver claim
            String tokenWithoutAuthVer = io.jsonwebtoken.Jwts.builder()
                    .issuer("elearning-backend")
                    .subject(email)
                    .claim("roles", List.of("Learner"))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000L))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-jwt-secret-key-for-testing-only-must-be-at-least-256-bits-long-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .compact();

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutAuthVer))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Admin role downgraded to Learner WHEN using previous Admin token THEN access to admin endpoint is blocked with 401")
        void test10_roleDowngradeBypassBlocked() throws Exception {
            // 1. User is an Admin
            String email = "downgraded_admin@example.com";
            Account account = createAccountWithProfile(email, "Admin", "Active");

            String adminToken = jwtUtil.generateToken(email, List.of("Admin"), account.getAuthorizationVersion());

            // 2. Admin role is revoked -> downgraded to Learner
            Role learnerRole = roleRepository.findByRoleName("Learner").orElseThrow();
            Role adminRole = roleRepository.findByRoleName("Admin").orElseThrow();
            account.removeRole(adminRole);
            account.addRole(learnerRole);
            account.incrementAuthorizationVersion();
            accountRepository.saveAndFlush(account);

            // 3. User attempts to use previous Admin token to access Admin radical endpoints
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("4. Transaction Rollback & Atomicity")
    class TransactionRollbackTests {

        @Test
        @DisplayName("GIVEN role mutation fails and rolls back WHEN checked THEN authorizationVersion remains unchanged and old token remains valid (Test 8)")
        void test8_transactionRollbackPreservesVersionAndOldToken() throws Exception {
            String email = "rollback_user@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");
            Long initialVersion = account.getAuthorizationVersion();

            String token = jwtUtil.generateToken(email, List.of("Learner"), initialVersion);

            // Execute a transactional mutation that throws an exception to trigger rollback
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            try {
                txTemplate.execute(status -> {
                    Account acc = accountRepository.findByEmailOrPhone(email).orElseThrow();
                    Role creatorRole = roleRepository.findByRoleName("Creator").orElseThrow();
                    acc.addRole(creatorRole);
                    acc.incrementAuthorizationVersion();
                    accountRepository.saveAndFlush(acc);

                    // Simulated failure mid-transaction
                    throw new RuntimeException("Simulated failure triggering rollback");
                });
            } catch (RuntimeException ignored) {
                // Expected rollback
            }

            // Verify account in DB has original version and roles
            Account reloaded = accountRepository.findByEmailOrPhone(email).orElseThrow();
            assertThat(reloaded.getAuthorizationVersion()).isEqualTo(initialVersion);
            assertThat(reloaded.getRoles())
                    .extracting(Role::getRoleName)
                    .containsExactly("Learner");

            // Old token remains completely valid
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }
}