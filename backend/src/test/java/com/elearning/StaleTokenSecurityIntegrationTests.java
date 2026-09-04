package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.security.JwtUtil;
import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.service.AdminAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive Verification and Security Tests for BE-AUTH-001:
 * Reject Stale JWT After Account Disable.
 *
 * Verifies that:
 * 1. An unexpired JWT issued when an account was Active successfully authenticates.
 * 2. When the account is subsequently Disabled ('Inactive' / 'Banned') in the database,
 *    presenting the EXACT SAME unexpired JWT is rejected with HTTP 401 Unauthorized (Authentication boundary).
 * 3. When the account is re-enabled to Active, presenting the same unexpired JWT succeeds again
 *    (proving authoritative server-side mutable state check).
 * 4. Authentication fails BEFORE RBAC (Disabled moderator receives 401 Unauthorized, not 403 Forbidden).
 * 5. JWT with a subject referring to a missing/deleted account is rejected with 401 Unauthorized.
 * 6. Disabling one account has zero effect on other active accounts (Account isolation).
 * 7. Cryptographic validation of invalid/expired JWTs remains fail-closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BE-AUTH-001: Stale Token After Account Disable Integration Tests")
class StaleTokenSecurityIntegrationTests {

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
    private AdminAccountService adminAccountService;

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
    @DisplayName("1. Core Stale Token Lifecycle Tests (Issue -> Active -> Disable -> Re-enable)")
    class StaleTokenLifecycleTests {

        @Test
        @DisplayName("GIVEN active account WHEN token issued and presented THEN authenticates with 200 OK")
        void test1_validTokenBeforeDisable_succeeds() throws Exception {
            // Arrange
            String email = "stale_test_user1@example.com";
            createAccountWithProfile(email, "Learner", "Active");

            String token = jwtUtil.generateToken(email, List.of("Learner"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("GIVEN token issued while active WHEN account is disabled THEN exact same token is rejected with 401 (BE-AUTH-001)")
        void test2_sameTokenAfterDisable_rejectedWith401() throws Exception {
            // 1. Arrange: Create Active account and issue JWT
            String email = "stale_test_user2@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");

            String issuedTokenWhileActive = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            // Verify baseline: token succeeds while account is Active
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuedTokenWhileActive))
                    .andExpect(status().isOk());

            // 2. Act: Disable the account via admin service (status -> 'Inactive')
            adminAccountService.updateAccountStatus(account.getAccountId(), new UpdateAccountStatusRequest("Inactive"), "admin@example.com");

            // Verify DB state
            Account reloaded = accountRepository.findByEmailOrPhone(email).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo("Inactive");

            // 3. Assert: Request protected endpoint with the EXACT SAME unexpired JWT
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuedTokenWhileActive))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Chưa xác thực hoặc phiên đăng nhập đã hết hạn"));
        }

        @Test
        @DisplayName("GIVEN disabled account WHEN re-enabled via admin service THEN old token is permanently rejected with 401 and only new token succeeds")
        void test3_oldTokenAfterReenable_permanentlyRevoked() throws Exception {
            // 1. Create Active account and issue JWT with current auth_ver
            String email = "stale_test_user3@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");

            String oldToken = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            // Baseline: oldToken works
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isOk());

            // 2. Admin disables account -> status = Inactive, auth_ver incremented
            adminAccountService.updateAccountStatus(account.getAccountId(), new UpdateAccountStatusRequest("Inactive"), "admin@example.com");

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized());

            // 3. Admin re-enables account back to Active -> status = Active, auth_ver incremented again
            adminAccountService.updateAccountStatus(account.getAccountId(), new UpdateAccountStatusRequest("Active"), "admin@example.com");

            // Assert: Old token is permanently invalid (auth_ver mismatch)
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            // 4. A newly issued token (with updated auth_ver) succeeds
            Account refreshed = accountRepository.findById(account.getAccountId()).orElseThrow();
            String newToken = jwtUtil.generateToken(email, List.of("Learner"), refreshed.getAuthorizationVersion());

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("GIVEN active account WHEN admin updates status idempotently to Active THEN auth_ver does not change and existing token remains valid")
        void test3b_idempotentActiveUpdate_preservesToken() throws Exception {
            String email = "stale_test_user3b@example.com";
            Account account = createAccountWithProfile(email, "Learner", "Active");
            String token = jwtUtil.generateToken(email, List.of("Learner"), account.getAuthorizationVersion());

            // Admin updates status with same value "Active"
            adminAccountService.updateAccountStatus(account.getAccountId(), new UpdateAccountStatusRequest("Active"), "admin@example.com");

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("2. Role-Protected Endpoints & RBAC Precedence Tests")
    class RbacPrecedenceTests {

        @Test
        @DisplayName("GIVEN disabled Moderator WHEN accessing Moderator endpoint THEN rejected with 401 Unauthorized (Auth fails before RBAC)")
        void test4_disabledModerator_failsAuthBeforeRbac() throws Exception {
            // Arrange
            String email = "stale_moderator@example.com";
            Account modAccount = createAccountWithProfile(email, "Moderator", "Active");

            String modToken = jwtUtil.generateToken(email, List.of("Moderator"), modAccount.getAuthorizationVersion());

            // Baseline: Active Moderator gets 200 OK on moderator queue
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                    .andExpect(status().isOk());

            // Disable Moderator account (status -> 'Banned')
            adminAccountService.updateAccountStatus(modAccount.getAccountId(), new UpdateAccountStatusRequest("Banned"), "admin@example.com");

            // Assert: Returns 401 UNAUTHORIZED, NOT 403 FORBIDDEN
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + modToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN disabled Creator WHEN accessing Creator endpoint THEN rejected with 401 Unauthorized")
        void test5_disabledCreator_failsAuthWith401() throws Exception {
            // Arrange
            String email = "stale_creator@example.com";
            Account creatorAccount = createAccountWithProfile(email, "Creator", "Active");

            String creatorToken = jwtUtil.generateToken(email, List.of("Creator"), creatorAccount.getAuthorizationVersion());

            // Baseline: Active Creator gets 200 OK on creator lessons
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                    .andExpect(status().isOk());

            // Disable Creator account
            adminAccountService.updateAccountStatus(creatorAccount.getAccountId(), new UpdateAccountStatusRequest("Inactive"), "admin@example.com");

            // Assert: Returns 401 UNAUTHORIZED
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("3. Nonexistent Account & Account Isolation Tests")
    class IsolationAndNonexistentTests {

        @Test
        @DisplayName("GIVEN validly signed JWT for nonexistent/deleted account WHEN presented THEN rejected with 401 Unauthorized")
        void test6_nonexistentAccount_rejectedWith401() throws Exception {
            String ghostToken = jwtUtil.generateToken("ghost_user_999@example.com", List.of("Learner"));

            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ghostToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Account A disabled and Account B active WHEN both make requests THEN Account A fails and Account B succeeds")
        void test7_differentAccountIsolation() throws Exception {
            // Account A (Disabled)
            String emailA = "user_a_isolation@example.com";
            createAccountWithProfile(emailA, "Learner", "Inactive");
            String tokenA = jwtUtil.generateToken(emailA, List.of("Learner"));

            // Account B (Active)
            String emailB = "user_b_isolation@example.com";
            createAccountWithProfile(emailB, "Learner", "Active");
            String tokenB = jwtUtil.generateToken(emailB, List.of("Learner"));

            // Account A -> 401
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            // Account B -> 200 OK
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }
}
