package com.elearning;

import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8D.1: Admin Account Management Integration Tests")
class AdminAccountIntegrationTests {

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

        adminAccount = accountRepository.findByEmailOrPhone("admin_8d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_8d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin8d11234567890123");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleAdmin);

                    UserProfile profile = new UserProfile();
                    profile.setFullName("Admin 8D1");
                    acc.setUserProfile(profile);
                    return accountRepository.save(acc);
                });

        targetAccount = accountRepository.findByEmailOrPhone("target_8d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("target_8d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashtarget8d1123456789012");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleLearner);

                    UserProfile profile = new UserProfile();
                    profile.setFullName("Target User 8D1");
                    acc.setUserProfile(profile);
                    return accountRepository.save(acc);
                });

        learnerAccount = accountRepository.findByEmailOrPhone("learner_8d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_8d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner8d112345678901");
                    acc.setStatus("Active");
                    acc.setAuthorizationVersion(1L);
                    acc.addRole(roleLearner);
                    return accountRepository.save(acc);
                });

        adminToken = jwtUtil.generateToken(adminAccount.getEmailOrPhone(), List.of("Admin"), adminAccount.getAuthorizationVersion());
        learnerToken = jwtUtil.generateToken(learnerAccount.getEmailOrPhone(), List.of("Learner"), learnerAccount.getAuthorizationVersion());
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts Tests")
    class GetAccountsIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling GET accounts THEN returns 401 Unauthorized")
        void testGetAccounts_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/accounts"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("GIVEN non-admin user WHEN calling GET accounts THEN returns 403 Forbidden")
        void testGetAccounts_nonAdmin_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/accounts")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("GIVEN admin user WHEN calling GET accounts THEN returns 200 with paginated list")
        void testGetAccounts_admin_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/accounts")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("GIVEN search and status filters WHEN calling GET accounts THEN returns filtered results")
        void testGetAccounts_filtered_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/accounts")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "Active")
                            .param("search", "Target User 8D1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items[0].emailOrPhone", is("target_8d1@example.com")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{id}/status Tests")
    class UpdateAccountStatusIntegrationTests {

        @Test
        @DisplayName("GIVEN valid status update WHEN calling PUT status THEN status updated in database")
        void testUpdateStatus_success() throws Exception {
            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Banned");

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Banned")));

            Account updated = accountRepository.findById(targetAccount.getAccountId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo("Banned");
        }

        @Test
        @DisplayName("GIVEN self-deactivation attempt by current admin WHEN calling PUT status THEN returns 400 Bad Request")
        void testSelfDeactivation_returns400() throws Exception {
            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Inactive");

            mockMvc.perform(put("/api/v1/admin/accounts/" + adminAccount.getAccountId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.message", is("Không thể tự vô hiệu hóa hoặc khóa tài khoản đang đăng nhập")));
        }

        @Test
        @DisplayName("GIVEN invalid status payload WHEN calling PUT status THEN returns 400 Validation Error")
        void testInvalidStatus_returns400() throws Exception {
            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("InvalidStatus");

            mockMvc.perform(put("/api/v1/admin/accounts/" + targetAccount.getAccountId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("GIVEN non-existent account ID WHEN calling PUT status THEN returns 404 Not Found")
        void testNonExistentAccount_returns404() throws Exception {
            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Inactive");

            mockMvc.perform(put("/api/v1/admin/accounts/999999/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }
}
