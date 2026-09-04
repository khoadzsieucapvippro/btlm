package com.elearning;

import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8D.2: Admin Role Management & Revocation Integration Tests")
class AdminRoleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Account adminAccount;
    private Account targetAccount;
    private Account learnerAccount;

    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() {
        Role roleLearner = roleRepository.findByRoleName("Learner").orElseGet(() -> roleRepository.save(new Role(1, "Learner")));
        Role roleAdmin = roleRepository.findByRoleName("Admin").orElseGet(() -> roleRepository.save(new Role(4, "Admin")));

        adminAccount = accountRepository.findByEmailOrPhone("admin_8d2@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_8d2@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin8d21234567890123");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleAdmin);
                    return accountRepository.save(acc);
                });

        targetAccount = accountRepository.findByEmailOrPhone("target_8d2@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("target_8d2@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashtarget8d2123456789012");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleLearner);

                    com.elearning.entity.UserProfile profile = new com.elearning.entity.UserProfile();
                    profile.setFullName("Target User 8D2");
                    acc.setUserProfile(profile);
                    return accountRepository.save(acc);
                });

        learnerAccount = accountRepository.findByEmailOrPhone("learner_8d2@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_8d2@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner8d212345678901");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleLearner);
                    return accountRepository.save(acc);
                });

        adminToken = jwtUtil.generateToken(adminAccount.getEmailOrPhone(), List.of("Admin"), adminAccount.getAuthorizationVersion());
        learnerToken = jwtUtil.generateToken(learnerAccount.getEmailOrPhone(), List.of("Learner"), learnerAccount.getAuthorizationVersion());
    }

    @Nested
    @DisplayName("GET /api/v1/admin/roles Tests")
    class GetRolesIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling GET roles THEN returns 401 Unauthorized")
        void testGetRoles_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/roles"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("GIVEN non-admin user WHEN calling GET roles THEN returns 403 Forbidden")
        void testGetRoles_nonAdmin_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/roles")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("GIVEN admin user WHEN calling GET roles THEN returns 200 with all 4 system roles")
        void testGetRoles_admin_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/roles")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[0].roleName", is("Learner")))
                    .andExpect(jsonPath("$.data[3].roleName", is("Admin")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{id}/roles Tests")
    class UpdateAccountRolesIntegrationTests {

        @Test
        @DisplayName("GIVEN effective role change WHEN updating roles THEN roles replaced, auth_ver incremented, and old token invalidated")
        void testUpdateRoles_effectiveChange_incrementsVersionAndRevokesOldTokens() throws Exception {
            String oldTargetToken = jwtUtil.generateToken(targetAccount.getEmailOrPhone(), List.of("Learner"), 1L);

            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "Creator"));

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Phân quyền tài khoản thành công")))
                    .andExpect(jsonPath("$.data.roles", hasSize(2)));

            Account updated = accountRepository.findById(targetAccount.getAccountId()).orElseThrow();
            assertThat(updated.getAuthorizationVersion()).isEqualTo(2L);

            // Verify old token with auth_ver=1 is now rejected by JwtAuthenticationFilter on protected route
            mockMvc.perform(get("/api/v1/users/profile")
                            .header("Authorization", "Bearer " + oldTargetToken))
                    .andExpect(status().isUnauthorized());

            // Fresh token with auth_ver=2 is accepted
            String freshToken = jwtUtil.generateToken(targetAccount.getEmailOrPhone(), List.of("Learner", "Creator"), 2L);
            mockMvc.perform(get("/api/v1/users/profile")
                            .header("Authorization", "Bearer " + freshToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GIVEN no-op role update WHEN updating identical roles THEN auth_ver remains unchanged")
        void testUpdateRoles_noOp_doesNotIncrementVersion() throws Exception {
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner"));

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")));

            Account updated = accountRepository.findById(targetAccount.getAccountId()).orElseThrow();
            assertThat(updated.getAuthorizationVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("GIVEN self-demotion attempt by admin WHEN removing Admin role THEN returns 400 Bad Request (POL-8D-02)")
        void testSelfDemotion_returns400() throws Exception {
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "Moderator"));

            mockMvc.perform(put("/api/v1/admin/accounts/" + adminAccount.getAccountId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.message", is("Không thể tự tước quyền Quản trị viên (Admin) của chính mình")));
        }

        @Test
        @DisplayName("GIVEN empty roles list WHEN updating THEN returns 400 Validation Error (BUS-8D-01)")
        void testEmptyRoles_returns400() throws Exception {
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of());

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("GIVEN invalid role name WHEN updating THEN returns 400 Bad Request")
        void testInvalidRole_returns400() throws Exception {
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "NonExistentRole"));

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
        }
    }
}
