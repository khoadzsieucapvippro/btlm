package com.elearning;

import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.request.UpdateProfileRequest;
import com.elearning.entity.UserProfile;
import com.elearning.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UserProfile Vertical Slice Integration Tests (Checkpoint 3C)")
class UserProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email, String password, String fullName) throws Exception {
        RegisterRequest regReq = new RegisterRequest(email, password, fullName);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest logReq = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return root.get("data").get("token").asText();
    }

    @Test
    @DisplayName("GIVEN unauthenticated request WHEN GET or PUT /api/v1/users/profile THEN returns 401 Unauthorized (Checkpoint 3C)")
    void testUnauthenticatedProfileAccessReturns401() throws Exception {
        // GET without JWT
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // PUT without JWT
        UpdateProfileRequest updateReq = new UpdateProfileRequest("New Name", null);
        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GIVEN two authenticated users WHEN user A updates profile THEN user B profile remains isolated and untouched")
    void testProfileOwnershipAndIsolation() throws Exception {
        long ts = System.currentTimeMillis();
        String emailA = "userA." + ts + "@elearning.com";
        String emailB = "userB." + ts + "@elearning.com";

        String tokenA = registerAndLogin(emailA, "passwordA123", "User A Original");
        String tokenB = registerAndLogin(emailB, "passwordB123", "User B Original");

        // 1. GET profile for User A
        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.emailOrPhone").value(emailA))
                .andExpect(jsonPath("$.data.fullName").value("User A Original"));

        // 2. GET profile for User B
        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.emailOrPhone").value(emailB))
                .andExpect(jsonPath("$.data.fullName").value("User B Original"));

        // 3. User A updates profile
        UpdateProfileRequest updateRequestA = new UpdateProfileRequest("User A Modified", "https://cdn.example.com/avatarA.png");
        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fullName").value("User A Modified"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatarA.png"));

        // 4. Verify User A gets updated profile
        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("User A Modified"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatarA.png"));

        // 5. Verify User B profile is completely untouched
        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("User B Original"))
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist());

        // 6. Direct Database Check (MySQL)
        Optional<UserProfile> profileAInDb = userProfileRepository.findByAccountEmailOrPhone(emailA);
        assertThat(profileAInDb).isPresent();
        assertThat(profileAInDb.get().getFullName()).isEqualTo("User A Modified");
        assertThat(profileAInDb.get().getAvatarUrl()).isEqualTo("https://cdn.example.com/avatarA.png");

        Optional<UserProfile> profileBInDb = userProfileRepository.findByAccountEmailOrPhone(emailB);
        assertThat(profileBInDb).isPresent();
        assertThat(profileBInDb.get().getFullName()).isEqualTo("User B Original");
    }
}
