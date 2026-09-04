package com.elearning;

import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 8C.1: Comprehensive Personalization Verification & Phase 8 Checkpoint Test Suite.
 * Validates the end-to-end integration and causal link across the full REST stack:
 *   UserSrsSettingController (PUT /api/v1/srs/settings)
 *        ↓
 *   UserSrsSettingService / MySQL user_srs_setting
 *        ↓
 *   SrsService / SrsController (GET /api/v1/srs/due, GET /api/v1/srs/stats)
 *        ↓
 *   Actual returned due-card count on real MySQL 8.4 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8C.1: Personalization Verification & Phase 8 Checkpoint Tests (MockMvc + MySQL 8.4)")
class Phase8PersonalizationIntegrationTests {

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

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    private UserProfile userA;
    private UserProfile userB;
    private String tokenA;
    private String tokenB;
    private List<Vocabulary> testVocabs;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        // 1. Create User A
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

        // 2. Create User B
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

        // 3. Create 20 test vocabularies for deterministic due card generation
        testVocabs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String pinyinRaw = "vocab" + suffixA + "_" + i;
            String hanzi = "字" + i;
            Vocabulary v = new Vocabulary(hanzi, "zì" + i, pinyinRaw, "Tự " + i, "Nghĩa từ " + i);
            testVocabs.add(vocabularyRepository.save(v));
        }
    }

    private void seedDueCardsForUser(UserProfile user, int count) {
        LocalDateTime past = LocalDateTime.now().minusHours(2);
        for (int i = 0; i < count && i < testVocabs.size(); i++) {
            Vocabulary v = testVocabs.get(i);
            CardProgress cp = new CardProgress(user, "VOCABULARY", v.getVocabId(), new BigDecimal("2.50"), 1, 1, past.plusMinutes(i));
            cardProgressRepository.save(cp);
        }
    }

    @Nested
    @DisplayName("1. Security Boundary Tests (Anonymous Access Denial)")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/settings returns 401")
        void testGetSettings_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/settings"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous PUT /api/v1/srs/settings returns 401")
        void testPutSettings_anonymous_returns401() throws Exception {
            UpdateSrsSettingRequest req = new UpdateSrsSettingRequest(20, 100);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/due returns 401")
        void testGetDue_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/due"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/stats returns 401")
        void testGetStats_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/stats"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("2. Primary Personalization Checkpoint Tests (Setting PUT -> Due Card Count Effect)")
    class PrimaryPersonalizationTests {

        @Test
        @DisplayName("Phase 8 Checkpoint: Updating maxReviewPerDay directly and dynamically changes returned due-card count in GET /api/v1/srs/due")
        void testSettingChange_modifiesDueCardCountDirectly() throws Exception {
            // Seed 15 due cards for User A
            seedDueCardsForUser(userA, 15);

            // Step 1: PUT settings with maxReviewPerDay = 3
            UpdateSrsSettingRequest req1 = new UpdateSrsSettingRequest(20, 3);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(3)));

            // Verify GET /api/v1/srs/due returns EXACTLY 3 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(3)));

            // Verify GET /api/v1/srs/stats reflects maxReviewLimit = 3
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(3)))
                    .andExpect(jsonPath("$.data.cardsDue", is(15)));

            // Step 2: PUT settings with maxReviewPerDay = 7
            UpdateSrsSettingRequest req2 = new UpdateSrsSettingRequest(20, 7);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(7)));

            // Verify GET /api/v1/srs/due returns EXACTLY 7 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(7)));

            // Step 3: PUT settings with maxReviewPerDay = 12
            UpdateSrsSettingRequest req3 = new UpdateSrsSettingRequest(20, 12);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req3)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(12)));

            // Verify GET /api/v1/srs/due returns EXACTLY 12 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(12)));

            // Step 4: PUT settings down to maxReviewPerDay = 2 (Reverse direction check)
            UpdateSrsSettingRequest req4 = new UpdateSrsSettingRequest(20, 2);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req4)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(2)));

            // Verify GET /api/v1/srs/due returns EXACTLY 2 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Limit Saturation Check: When configured limit exceeds eligible cards in DB, returns all available cards without error")
        void testLimitSaturation_returnsAllAvailableCards() throws Exception {
            // Seed 5 due cards for User A
            seedDueCardsForUser(userA, 5);

            // PUT settings with maxReviewPerDay = 50 (exceeds 5 available cards)
            UpdateSrsSettingRequest req = new UpdateSrsSettingRequest(30, 50);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            // GET /api/v1/srs/due returns all 5 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(5)));
        }

        @Test
        @DisplayName("newCardsPerDay End-to-End: Updates via PUT /srs/settings, persists to MySQL, and propagates directly to GET /srs/stats with user isolation")
        void testNewCardsPerDay_persistsAndPropagatesToStatsAcrossFullStack() throws Exception {
            // Step 1: Default stats check (newCardsLimit = 20)
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsLimit", is(20)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(100)));

            // Step 2: PUT /api/v1/srs/settings with newCardsPerDay = 45, maxReviewPerDay = 150
            UpdateSrsSettingRequest req1 = new UpdateSrsSettingRequest(45, 150);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(45)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(150)));

            // Step 3: Verify GET /api/v1/srs/settings returns newCardsPerDay = 45
            mockMvc.perform(get("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(45)));

            // Step 4: Verify GET /api/v1/srs/stats returns newCardsLimit = 45
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsLimit", is(45)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(150)));

            // Step 5: Reverse direction check - PUT newCardsPerDay = 10, maxReviewPerDay = 60
            UpdateSrsSettingRequest req2 = new UpdateSrsSettingRequest(10, 60);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(10)));

            // Step 6: Verify stats updated to 10
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsLimit", is(10)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(60)));

            // Step 7: User isolation for newCardsLimit
            UpdateSrsSettingRequest reqB = new UpdateSrsSettingRequest(30, 100);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqB)))
                    .andExpect(status().isOk());

            // User A stats remains 10
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsLimit", is(10)));

            // User B stats is 30
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newCardsLimit", is(30)));
        }
    }

    @Nested
    @DisplayName("3. Daily Quota Exhaustion & Recovery Verification")
    class DailyQuotaExhaustionTests {

        @Test
        @DisplayName("Daily reviews counted against maxReviewPerDay; increasing setting immediately restores due card availability")
        void testDailyQuotaExhaustion_andRecoveryOnLimitIncrease() throws Exception {
            // Seed 10 due cards for User A
            seedDueCardsForUser(userA, 10);

            // Set maxReviewPerDay = 3
            UpdateSrsSettingRequest req1 = new UpdateSrsSettingRequest(20, 3);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk());

            // Simulate 3 completed reviews today
            reviewLogRepository.save(new ReviewLog(userA, "VOCABULARY", testVocabs.get(0).getVocabId(), 3, 1, 3, 4));
            reviewLogRepository.save(new ReviewLog(userA, "VOCABULARY", testVocabs.get(1).getVocabId(), 4, 1, 6, 3));
            reviewLogRepository.save(new ReviewLog(userA, "VOCABULARY", testVocabs.get(2).getVocabId(), 2, 1, 1, 5));

            // Quota exhausted (3 reviews out of 3 limit) -> GET /srs/due returns 0 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            // Stats show reviewsToday = 3, maxReviewLimit = 3
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reviewsToday", is(3)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(3)));

            // Learner increases limit to maxReviewPerDay = 8 -> Remaining quota = 8 - 3 = 5 cards
            UpdateSrsSettingRequest req2 = new UpdateSrsSettingRequest(20, 8);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk());

            // GET /api/v1/srs/due immediately returns exactly 5 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(5)));
        }
    }

    @Nested
    @DisplayName("4. Cross-User Personalization & Isolation Tests")
    class UserIsolationTests {

        @Test
        @DisplayName("User A settings modifications do not affect User B settings or due cards")
        void testCrossUserPersonalizationIsolation() throws Exception {
            // Seed 15 due cards for User A and 15 due cards for User B
            seedDueCardsForUser(userA, 15);
            seedDueCardsForUser(userB, 15);

            // User A sets limit = 4
            UpdateSrsSettingRequest reqA = new UpdateSrsSettingRequest(25, 4);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqA)))
                    .andExpect(status().isOk());

            // User B sets limit = 9
            UpdateSrsSettingRequest reqB = new UpdateSrsSettingRequest(35, 9);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqB)))
                    .andExpect(status().isOk());

            // Verify User A receives exactly 4 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(4)));

            // Verify User B receives exactly 9 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(9)));

            // User A increases limit to 11
            UpdateSrsSettingRequest reqA2 = new UpdateSrsSettingRequest(25, 11);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqA2)))
                    .andExpect(status().isOk());

            // User A now receives 11 cards
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(11)));

            // User B STILL receives 9 cards (complete isolation)
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(9)));
        }
    }

    @Nested
    @DisplayName("5. Validation & Non-Mutation Regression Tests")
    class ValidationProtectionTests {

        @Test
        @DisplayName("Invalid PUT request (zero value) returns 400 and does NOT alter existing due card limits")
        void testInvalidSettingPUT_doesNotMutateDueCardLimits() throws Exception {
            // Seed 10 due cards for User A
            seedDueCardsForUser(userA, 10);

            // Establish valid setting: maxReviewPerDay = 4
            UpdateSrsSettingRequest validReq = new UpdateSrsSettingRequest(20, 4);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReq)))
                    .andExpect(status().isOk());

            // Verify due count is 4
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(4)));

            // Send invalid PUT (zero maxReviewPerDay)
            UpdateSrsSettingRequest invalidReq = new UpdateSrsSettingRequest(20, 0);
            mockMvc.perform(put("/api/v1/srs/settings")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            // Verify due count remains strictly 4 (no DB corruption or mutation)
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(4)));
        }
    }
}
