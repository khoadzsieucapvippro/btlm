package com.elearning;

import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8B.1: User SRS Settings REST API Integration Tests (MockMvc + MySQL 8.4)")
class UserSrsSettingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    private UserProfile userA;
    private UserProfile userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        // Create User A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        Account accA = new Account();
        accA.setEmailOrPhone("learner_a_" + suffixA + "@test.com");
        accA.setPasswordHash("hash");
        accA.setStatus("Active");
        if (learnerRole != null) {
            accA.getRoles().add(learnerRole);
        }
        accA = accountRepository.save(accA);

        userA = new UserProfile();
        userA.setAccount(accA);
        userA.setFullName("Learner A " + suffixA);
        userA = userProfileRepository.save(userA);
        tokenA = jwtUtil.generateToken(accA.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // Create User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accB = new Account();
        accB.setEmailOrPhone("learner_b_" + suffixB + "@test.com");
        accB.setPasswordHash("hash");
        accB.setStatus("Active");
        if (learnerRole != null) {
            accB.getRoles().add(learnerRole);
        }
        accB = accountRepository.save(accB);

        userB = new UserProfile();
        userB.setAccount(accB);
        userB.setFullName("Learner B " + suffixB);
        userB = userProfileRepository.save(userB);
        tokenB = jwtUtil.generateToken(accB.getEmailOrPhone(), List.of("ROLE_LEARNER"));
    }

    @Nested
    @DisplayName("1. Security Boundary Tests (Anonymous vs Authenticated)")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/settings returns 401 Unauthorized")
        void testGetSettings_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/settings"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous PUT /api/v1/srs/settings returns 401 Unauthorized")
        void testPutSettings_anonymous_returns401() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(30, 120);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("2. Checkpoint 8B Vertical Slice & User Isolation Tests")
    class Checkpoint8BTests {

        @Test
        @DisplayName("Checkpoint 8B.1: First-time learner GET returns default settings (20 new cards / 100 max reviews)")
        void testGetSettings_firstTime_returnsDefaults() throws Exception {
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(20)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(100)));

            // Verify persistence in MySQL
            UserSrsSetting persisted = userSrsSettingRepository.findByUser(userA).orElseThrow();
            assertThat(persisted.getNewCardsPerDay()).isEqualTo(20);
            assertThat(persisted.getMaxReviewPerDay()).isEqualTo(100);
        }

        @Test
        @DisplayName("Checkpoint 8B.2: Learner PUT updates settings and persists to MySQL")
        void testPutSettings_success() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(35, 125);

            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(35)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(125)));

            // Independent Database verification
            UserSrsSetting settingOnDb = userSrsSettingRepository.findByUser(userA).orElseThrow();
            assertThat(settingOnDb.getNewCardsPerDay()).isEqualTo(35);
            assertThat(settingOnDb.getMaxReviewPerDay()).isEqualTo(125);
        }

        @Test
        @DisplayName("Checkpoint 8B.3: Validation - zero or negative integers return 400 and do not mutate DB")
        void testPutSettings_invalidNonPositive_returns400_noMutation() throws Exception {
            // Setup initial valid setting
            userSrsSettingRepository.save(new UserSrsSetting(userA, 25, 110));

            // Attempt zero newCardsPerDay
            UpdateSrsSettingRequest zeroNewCards = new UpdateSrsSettingRequest(0, 110);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(zeroNewCards)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            // Attempt negative maxReviewPerDay
            UpdateSrsSettingRequest negativeMaxReview = new UpdateSrsSettingRequest(25, -10);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(negativeMaxReview)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            // Verify DB state unchanged
            UserSrsSetting settingOnDb = userSrsSettingRepository.findByUser(userA).orElseThrow();
            assertThat(settingOnDb.getNewCardsPerDay()).isEqualTo(25);
            assertThat(settingOnDb.getMaxReviewPerDay()).isEqualTo(110);
        }

        @Test
        @DisplayName("User Isolation: User A updating settings does not affect User B settings")
        void testUserIsolation() throws Exception {
            // User A updates to 40 / 150
            UpdateSrsSettingRequest reqA = new UpdateSrsSettingRequest(40, 150);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqA)))
                    .andExpect(status().isOk());

            // User B updates to 50 / 200
            UpdateSrsSettingRequest reqB = new UpdateSrsSettingRequest(50, 200);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqB)))
                    .andExpect(status().isOk());

            // User A retrieves settings -> 40 / 150
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(40)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(150)));

            // User B retrieves settings -> 50 / 200
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(50)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(200)));
        }

        @Test
        @DisplayName("Full Lifecycle: GET default (20/100) -> PUT (35/125) -> GET (35/125) -> PUT (15/80) -> GET (15/80)")
        void testFullLifecycle() throws Exception {
            // 1. Initial GET -> 20 / 100
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(20)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(100)));

            // 2. PUT -> 35 / 125
            UpdateSrsSettingRequest req1 = new UpdateSrsSettingRequest(35, 125);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(35)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(125)));

            // 3. GET -> 35 / 125
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(35)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(125)));

            // 4. PUT -> 15 / 80
            UpdateSrsSettingRequest req2 = new UpdateSrsSettingRequest(15, 80);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(15)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(80)));

            // 5. GET -> 15 / 80
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(15)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(80)));
        }
    }
}
