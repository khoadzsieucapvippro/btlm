package com.elearning;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End MockMvc Integration Tests for SRS REST API (Task 7B.2 & Checkpoint 7B).
 * Verifies:
 * - Security & RBAC: Anonymous access returns 401 UNAUTHORIZED, Learner & Admin access succeed (200 OK).
 * - Full Checkpoint 7B Vertical Slice on real MySQL database:
 *   Authenticated Learner -> GET due -> POST review (rating 3, 4s) -> DB CARD_PROGRESS & REVIEW_LOG verification -> GET stats.
 * - Validation errors (400 Bad Request) & business errors (404 Not Found) over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 7B.2: SrsController MockMvc Integration & Checkpoint 7B Tests")
class SrsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UserProfile learnerUser;
    private UserProfile adminUser;
    private String learnerToken;
    private String adminToken;
    private Vocabulary testVocab;
    private Radical testRadical;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
        Role adminRole = roleRepository.findByRoleName("Admin").orElse(null);

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Setup Learner
        Account learnerAccount = new Account();
        learnerAccount.setEmailOrPhone("srs_learner_" + suffix + "@test.com");
        learnerAccount.setPasswordHash("hash");
        learnerAccount.setStatus("Active");
        if (learnerRole != null) {
            learnerAccount.getRoles().add(learnerRole);
        }
        learnerAccount = accountRepository.save(learnerAccount);

        learnerUser = new UserProfile();
        learnerUser.setAccount(learnerAccount);
        learnerUser.setFullName("SRS Learner " + suffix);
        learnerUser = userProfileRepository.save(learnerUser);
        learnerToken = jwtUtil.generateToken(learnerAccount.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // Setup Admin
        Account adminAccount = new Account();
        adminAccount.setEmailOrPhone("srs_admin_" + suffix + "@test.com");
        adminAccount.setPasswordHash("hash");
        adminAccount.setStatus("Active");
        if (adminRole != null) {
            adminAccount.getRoles().add(adminRole);
        }
        adminAccount = accountRepository.save(adminAccount);

        adminUser = new UserProfile();
        adminUser.setAccount(adminAccount);
        adminUser.setFullName("SRS Admin " + suffix);
        adminUser = userProfileRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminAccount.getEmailOrPhone(), List.of("ROLE_ADMIN"));

        // Find or create test vocabulary
        testVocab = vocabularyRepository.findByHanziAndPinyinRaw("字", "zi")
                .orElseGet(() -> {
                    Vocabulary v = new Vocabulary("字", "zì", "zi", "Tự", "Chữ");
                    v.setExampleSentence("写字");
                    v.setExampleTranslation("Viết chữ");
                    return vocabularyRepository.save(v);
                });

        testRadical = radicalRepository.findAll().stream().findFirst()
                .orElseGet(() -> radicalRepository.save(new Radical(1, "一", "yī", "Nhất", "Số một")));
    }

    @Nested
    @DisplayName("1. Security & RBAC Boundary Tests")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/due returns 401 UNAUTHORIZED")
        void testGetDue_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/due")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/srs/review returns 401 UNAUTHORIZED")
        void testReview_anonymous_returns401() throws Exception {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4);

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/srs/stats returns 401 UNAUTHORIZED")
        void testGetStats_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/srs/stats")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Authenticated Admin GET /api/v1/srs/stats is allowed and returns 200 OK")
        void testGetStats_admin_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + adminToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.newCardsLimit", greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    @DisplayName("2. Checkpoint 7B: Full SRS Vertical Slice Tests")
    class Checkpoint7BVerticalSliceTests {

        @Test
        @DisplayName("Full Review Cycle: Due -> Review (Rating 3, 4s) -> DB State Verification -> Updated Stats")
        void testFullReviewVerticalSlice_persistsCorrectly() throws Exception {
            // Step 1: Create a due card for learnerUser
            CardProgress initialProgress = new CardProgress(
                    learnerUser,
                    "VOCABULARY",
                    testVocab.getVocabId(),
                    new BigDecimal("2.50"),
                    0,
                    0,
                    LocalDateTime.now().minusHours(2)
            );
            cardProgressRepository.save(initialProgress);

            // Step 2: Call GET /api/v1/srs/due
            mockMvc.perform(get("/api/v1/srs/due")
                            .header("Authorization", "Bearer " + learnerToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data[0].itemType", is("VOCABULARY")))
                    .andExpect(jsonPath("$.data[0].itemId", is(testVocab.getVocabId().intValue())))
                    .andExpect(jsonPath("$.data[0].hanzi", is(testVocab.getHanzi())));

            // Step 3: Call POST /api/v1/srs/review (Rating 3 = Good, 4 seconds)
            ReviewCardRequest reviewRequest = new ReviewCardRequest(
                    "VOCABULARY",
                    testVocab.getVocabId(),
                    3,
                    4
            );

            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.itemType", is("VOCABULARY")))
                    .andExpect(jsonPath("$.data.itemId", is(testVocab.getVocabId().intValue())))
                    .andExpect(jsonPath("$.data.intervalDays", is(1)))
                    .andExpect(jsonPath("$.data.repetitions", is(1)))
                    .andExpect(jsonPath("$.data.easeFactor", is(2.50)));

            // Step 4: Independent Database Assertions on CARD_PROGRESS
            CardProgress updatedProgress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(learnerUser, "VOCABULARY", testVocab.getVocabId())
                    .orElseThrow();
            assertThat(updatedProgress.getIntervalDays()).isEqualTo(1);
            assertThat(updatedProgress.getRepetitions()).isEqualTo(1);
            assertThat(updatedProgress.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));
            assertThat(updatedProgress.getNextReviewAt()).isAfter(LocalDateTime.now().plusHours(23));

            // Step 5: Independent Database Assertions on REVIEW_LOG
            List<ReviewLog> logs = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(learnerUser.getUserId()))
                    .toList();
            assertThat(logs).hasSize(1);
            ReviewLog log = logs.get(0);
            assertThat(log.getItemType()).isEqualTo("VOCABULARY");
            assertThat(log.getItemId()).isEqualTo(testVocab.getVocabId());
            assertThat(log.getRating()).isEqualTo((byte) 3);
            assertThat(log.getIntervalBefore()).isEqualTo(0);
            assertThat(log.getIntervalAfter()).isEqualTo(1);
            assertThat(log.getReviewTimeSeconds()).isEqualTo(4);
            assertThat(log.getReviewedAt()).isNotNull();

            // Step 6: Call GET /api/v1/srs/stats and verify stats reflect the completed review
            mockMvc.perform(get("/api/v1/srs/stats")
                            .header("Authorization", "Bearer " + learnerToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.reviewsToday", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data.newCardsLimit", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Review Radical via REST: Persists CARD_PROGRESS and REVIEW_LOG with correct polymorphic ID")
        void testReviewRadical_persistsCorrectly() throws Exception {
            ReviewCardRequest radicalRequest = new ReviewCardRequest(
                    "RADICAL",
                    (long) testRadical.getRadicalId(),
                    4, // Rating 4 = Easy -> interval 1, repetitions 1, EF 2.60
                    2
            );

            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(radicalRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.itemType", is("RADICAL")))
                    .andExpect(jsonPath("$.data.itemId", is(testRadical.getRadicalId())))
                    .andExpect(jsonPath("$.data.intervalDays", is(1)))
                    .andExpect(jsonPath("$.data.easeFactor", is(2.60)));

            CardProgress radicalProgress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(learnerUser, "RADICAL", (long) testRadical.getRadicalId())
                    .orElseThrow();
            assertThat(radicalProgress.getIntervalDays()).isEqualTo(1);
            assertThat(radicalProgress.getRepetitions()).isEqualTo(1);
            assertThat(radicalProgress.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.60"));
        }
    }

    @Nested
    @DisplayName("3. HTTP Input Validation & Business Error Propagation Tests")
    class ValidationAndBusinessErrorTests {

        @Test
        @DisplayName("POST /api/v1/srs/review with invalid rating (5) returns 400 VALIDATION_ERROR")
        void testReview_invalidRating_returns400() throws Exception {
            ReviewCardRequest invalidRequest = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 5, 3);

            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with invalid itemType ('LESSON') returns 400 VALIDATION_ERROR")
        void testReview_invalidItemType_returns400() throws Exception {
            ReviewCardRequest invalidRequest = new ReviewCardRequest("LESSON", testVocab.getVocabId(), 3, 3);

            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with nonexistent item ID returns 404 NOT_FOUND")
        void testReview_nonexistentItem_returns404() throws Exception {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 999999L, 3, 3);

            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with malformed JSON returns 400 BAD_REQUEST")
        void testReview_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/srs/review")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ malformed json }"))
                    .andExpect(status().isBadRequest());
        }
    }
}
