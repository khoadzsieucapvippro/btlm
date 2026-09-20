package com.elearning;

import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AuthIntegrationTests.TestProtectedConfig.class)
@DisplayName("Authentication & Registration Vertical Slice Integration Tests")
class AuthIntegrationTests {

    @TestConfiguration
    static class TestProtectedConfig {
        @RestController
        static class ProtectedEndpointController {
            @GetMapping("/api/v1/protected/ping")
            public String ping() {
                return "pong";
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Vertical Slice: Register -> MySQL check -> Duplicate Check -> Login -> JWT Validation -> JWT Handoff")
    void testCompleteAuthenticationVerticalSlice() throws Exception {
        String testEmail = "testlearner." + System.currentTimeMillis() + "@elearning.com";
        String rawPassword = "validPassword123";
        String fullName = "Nguyen Van Test";

        // ---------------------------------------------------------------------
        // 1. REGISTER
        // ---------------------------------------------------------------------
        RegisterRequest registerRequest = new RegisterRequest(testEmail, rawPassword, fullName);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andExpect(jsonPath("$.data.emailOrPhone").value(testEmail))
                .andExpect(jsonPath("$.data.fullName").value(fullName))
                .andExpect(jsonPath("$.data.roles[0]").value("Learner"))
                .andReturn();

        // ---------------------------------------------------------------------
        // 2. DATABASE VERIFICATION (MySQL JPA Entities)
        // ---------------------------------------------------------------------
        Optional<Account> savedAccountOpt = accountRepository.findByEmailOrPhone(testEmail);
        assertThat(savedAccountOpt).isPresent();
        Account savedAccount = savedAccountOpt.get();

        // Password must be BCrypt-hashed, NOT plaintext
        assertThat(savedAccount.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, savedAccount.getPasswordHash())).isTrue();

        // Status must be Active
        assertThat(savedAccount.getStatus()).isEqualTo("Active");

        // Role Learner must be assigned
        assertThat(savedAccount.getRoles())
                .extracting(Role::getRoleName)
                .containsExactly("Learner");

        // UserProfile must be cascaded and contain fullName
        assertThat(savedAccount.getUserProfile()).isNotNull();
        assertThat(savedAccount.getUserProfile().getFullName()).isEqualTo(fullName);

        // ---------------------------------------------------------------------
        // 3. DUPLICATE REGISTRATION REJECTION
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // ---------------------------------------------------------------------
        // 4. LOGIN SUCCESS
        // ---------------------------------------------------------------------
        LoginRequest loginRequest = new LoginRequest(testEmail, rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andExpect(jsonPath("$.data.emailOrPhone").value(testEmail))
                .andExpect(jsonPath("$.data.fullName").value(fullName))
                .andExpect(jsonPath("$.data.roles[0]").value("Learner"))
                .andReturn();

        // ---------------------------------------------------------------------
        // 5. REAL JWT TOKEN CRYPTOGRAPHIC & CLAIMS VERIFICATION
        // ---------------------------------------------------------------------
        JsonNode rootNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = rootNode.get("data").get("token").asText();

        assertThat(jwtToken).isNotBlank();
        assertThat(jwtUtil.validateToken(jwtToken)).isTrue();
        assertThat(jwtUtil.extractSubject(jwtToken)).isEqualTo(testEmail);
        assertThat(jwtUtil.extractRoles(jwtToken)).containsExactly("Learner");

        // ---------------------------------------------------------------------
        // 6. JWT FILTER HANDOFF (Token accepted by Spring Security on protected route)
        // ---------------------------------------------------------------------
        // Without token -> 401 Unauthorized (standard H-01 contract)
        mockMvc.perform(get("/api/v1/protected/ping"))
                .andExpect(status().isUnauthorized());

        // With valid token -> 200 OK
        mockMvc.perform(get("/api/v1/protected/ping")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // ---------------------------------------------------------------------
        // 7. LOGIN FAILURE: WRONG PASSWORD (401 Generic Error)
        // ---------------------------------------------------------------------
        LoginRequest wrongPassRequest = new LoginRequest(testEmail, "wrongPass456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // ---------------------------------------------------------------------
        // 8. LOGIN FAILURE: UNKNOWN ACCOUNT (401 Same Generic Error)
        // ---------------------------------------------------------------------
        LoginRequest unknownAccountRequest = new LoginRequest("unknown.user@elearning.com", "anyPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownAccountRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
