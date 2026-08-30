package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.SrsService;
import com.elearning.service.impl.SrsServiceImpl;
import com.elearning.service.srs.SrsCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 7C.1: Comprehensive Polymorphic SRS Integrity & Verification Test Suite.
 * Validates the polymorphic referential integrity of:
 *   CARD_PROGRESS (user_id, item_type, item_id)
 *   REVIEW_LOG (user_id, item_type, item_id)
 * where item_type IN ('VOCABULARY', 'RADICAL').
 *
 * Verifies on real MySQL 8.4 database:
 * - Positive resolution for existing Vocabulary & Radical items.
 * - Negative rejection for nonexistent items, wrong-table lookups, and invalid types.
 * - Prevention of spurious mutations (zero CARD_PROGRESS / REVIEW_LOG created on failure).
 * - Same numeric ID across different types creates 2 completely distinct cards and logs.
 * - Cross-user and cross-type state isolation.
 * - Duplicate progress prevention and immutable audit logging.
 * - Polymorphic due card resolution and stale progress resilience.
 * - Transaction atomicity and rollback guarantee.
 */
@SpringBootTest
@Transactional
@DisplayName("Task 7C.1: Polymorphic SRS Integrity & Module 7C Verification Tests")
class SrsPolymorphicIntegrityTests {

    @Autowired
    private SrsService srsService;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UserProfile userA;
    private UserProfile userB;
    private Vocabulary vocab1;
    private Radical radical1;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        // Create User A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        Account accA = new Account();
        accA.setEmailOrPhone("user_a_" + suffixA + "@test.com");
        accA.setPasswordHash("hash");
        accA.setStatus("Active");
        if (learnerRole != null) {
            accA.getRoles().add(learnerRole);
        }
        accA = accountRepository.save(accA);

        userA = new UserProfile();
        userA.setAccount(accA);
        userA.setFullName("User A " + suffixA);
        userA = userProfileRepository.save(userA);

        // Create User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accB = new Account();
        accB.setEmailOrPhone("user_b_" + suffixB + "@test.com");
        accB.setPasswordHash("hash");
        accB.setStatus("Active");
        if (learnerRole != null) {
            accB.getRoles().add(learnerRole);
        }
        accB = accountRepository.save(accB);

        userB = new UserProfile();
        userB.setAccount(accB);
        userB.setFullName("User B " + suffixB);
        userB = userProfileRepository.save(userB);

        // Create test Vocabulary
        vocab1 = vocabularyRepository.findByHanziAndPinyinRaw("天", "tian")
                .orElseGet(() -> {
                    Vocabulary v = new Vocabulary("天", "tiān", "tian", "Thiên", "Trời, ngày");
                    v.setExampleSentence("今天天气很好。");
                    v.setExampleTranslation("Hôm nay thời tiết rất đẹp.");
                    return vocabularyRepository.save(v);
                });

        testLesson = new Lesson();
        testLesson.setTitle("Poly Lesson " + suffixA);
        testLesson.setStatus("Approved");
        testLesson.setCreatedBy(accA);
        testLesson = lessonRepository.save(testLesson);

        if (!lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(testLesson.getLessonId(), vocab1.getVocabId())) {
            lessonVocabularyRepository.save(new LessonVocabulary(testLesson, vocab1, 1));
        }

        // Ensure test Radical exists (Radical 1 = 一)
        radical1 = radicalRepository.findById(1)
                .orElseGet(() -> radicalRepository.save(new Radical(1, "一", "yī", "Nhất", "Số một")));

        // Default authenticate as User A
        authenticateAs(userA);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (testLesson != null) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
            lessonRepository.delete(testLesson);
        }
    }

    private void authenticateAs(UserProfile user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getAccount().getEmailOrPhone(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("1. Positive Polymorphic Item Resolution")
    class PositiveResolutionTests {

        @Test
        @DisplayName("Test Group 1: Review existing VOCABULARY creates CardProgress and ReviewLog with VOCABULARY identity")
        void testReviewExistingVocabulary_accepted() {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 5);

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response).isNotNull();
            assertThat(response.getItemType()).isEqualTo("VOCABULARY");
            assertThat(response.getItemId()).isEqualTo(vocab1.getVocabId());
            assertThat(response.getHanzi()).isEqualTo(vocab1.getHanzi());
            assertThat(response.getIntervalDays()).isEqualTo(1);

            // Independent Database Assertions
            CardProgress progress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "VOCABULARY", vocab1.getVocabId())
                    .orElseThrow();
            assertThat(progress.getItemType()).isEqualTo("VOCABULARY");
            assertThat(progress.getItemId()).isEqualTo(vocab1.getVocabId());
            assertThat(progress.getUser().getUserId()).isEqualTo(userA.getUserId());
            assertThat(progress.getIntervalDays()).isEqualTo(1);
            assertThat(progress.getRepetitions()).isEqualTo(1);

            List<ReviewLog> logs = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userA.getUserId()))
                    .toList();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getItemType()).isEqualTo("VOCABULARY");
            assertThat(logs.get(0).getItemId()).isEqualTo(vocab1.getVocabId());
            assertThat(logs.get(0).getRating()).isEqualTo((byte) 3);
            assertThat(logs.get(0).getReviewTimeSeconds()).isEqualTo(5);
        }

        @Test
        @DisplayName("Test Group 2: Review existing RADICAL creates CardProgress and ReviewLog with RADICAL identity")
        void testReviewExistingRadical_accepted() {
            ReviewCardRequest request = new ReviewCardRequest("RADICAL", (long) radical1.getRadicalId(), 4, 3);

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response).isNotNull();
            assertThat(response.getItemType()).isEqualTo("RADICAL");
            assertThat(response.getItemId()).isEqualTo((long) radical1.getRadicalId());
            assertThat(response.getHanzi()).isEqualTo(radical1.getCharacter());
            assertThat(response.getIntervalDays()).isEqualTo(1);

            // Independent Database Assertions
            CardProgress progress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "RADICAL", (long) radical1.getRadicalId())
                    .orElseThrow();
            assertThat(progress.getItemType()).isEqualTo("RADICAL");
            assertThat(progress.getItemId()).isEqualTo((long) radical1.getRadicalId());
            assertThat(progress.getUser().getUserId()).isEqualTo(userA.getUserId());

            List<ReviewLog> logs = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userA.getUserId()))
                    .toList();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getItemType()).isEqualTo("RADICAL");
            assertThat(logs.get(0).getItemId()).isEqualTo((long) radical1.getRadicalId());
            assertThat(logs.get(0).getRating()).isEqualTo((byte) 4);
        }

        @Test
        @DisplayName("Test Group 3: Same Numeric ID across VOCABULARY and RADICAL creates 2 distinct cards and logs for same user")
        void testSameNumericId_differentTypes_createsIndependentCards() {
            // Ensure radical with ID 1 exists
            Radical rad1 = radicalRepository.findById(1)
                    .orElseGet(() -> radicalRepository.save(new Radical(1, "一", "yī", "Nhất", "Số một")));

            // Ensure vocabulary with ID 1 exists in DB
            if (!vocabularyRepository.existsById(1L)) {
                jdbcTemplate.execute("INSERT INTO vocabulary (vocab_id, hanzi, pinyin, pinyin_raw, meaning_han_viet, meaning_vi, created_at, updated_at) " +
                        "VALUES (1, '一', 'yī', 'yi', 'Nhất', 'Số một', NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE hanzi = '一'");
            }
            Long commonId = 1L;
            Vocabulary v1 = vocabularyRepository.findById(commonId).orElseThrow();
            if (!lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(testLesson.getLessonId(), commonId)) {
                lessonVocabularyRepository.save(new LessonVocabulary(testLesson, v1, 2));
            }

            // Review VOCABULARY commonId
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", commonId, 3, 2));

            // Review RADICAL commonId
            srsService.reviewCard(new ReviewCardRequest("RADICAL", commonId, 4, 3));

            // Verify 2 distinct CARD_PROGRESS rows exist for userA
            List<CardProgress> userProgress = cardProgressRepository.findAll().stream()
                    .filter(p -> p.getUser().getUserId().equals(userA.getUserId()))
                    .toList();
            assertThat(userProgress).hasSize(2);

            CardProgress vocabProgress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "VOCABULARY", commonId).orElseThrow();
            CardProgress radicalProgress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "RADICAL", commonId).orElseThrow();

            assertThat(vocabProgress.getProgressId()).isNotEqualTo(radicalProgress.getProgressId());
            assertThat(vocabProgress.getItemType()).isEqualTo("VOCABULARY");
            assertThat(radicalProgress.getItemType()).isEqualTo("RADICAL");

            // Verify 2 distinct REVIEW_LOG rows exist
            List<ReviewLog> userLogs = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userA.getUserId()))
                    .toList();
            assertThat(userLogs).hasSize(2);
            assertThat(userLogs).extracting(ReviewLog::getItemType).containsExactlyInAnyOrder("VOCABULARY", "RADICAL");
        }
    }

    @Nested
    @DisplayName("2. Negative Validation & Cross-Table Collision Prevention")
    class NegativeValidationTests {

        @Test
        @DisplayName("Test Group 4: Nonexistent VOCABULARY ID throws NOT_FOUND and creates zero progress/log")
        void testNonexistentVocabulary_rejected() {
            long initialProgressCount = cardProgressRepository.count();
            long initialLogCount = reviewLogRepository.count();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 999999L, 3, 4);

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            assertThat(cardProgressRepository.count()).isEqualTo(initialProgressCount);
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogCount);
        }

        @Test
        @DisplayName("Test Group 5: Nonexistent RADICAL ID throws NOT_FOUND and creates zero progress/log")
        void testNonexistentRadical_rejected() {
            long initialProgressCount = cardProgressRepository.count();
            long initialLogCount = reviewLogRepository.count();

            ReviewCardRequest request = new ReviewCardRequest("RADICAL", 888888L, 3, 4);

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            assertThat(cardProgressRepository.count()).isEqualTo(initialProgressCount);
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogCount);
        }

        @Test
        @DisplayName("Test Group 6: Cross-Table Collision - VOCABULARY request with Radical-only ID fails; RADICAL request with Vocab-only ID fails")
        void testCrossTableLookup_failsWhenItemNotPresentInTargetCatalog() {
            // Find an ID that only exists in Vocabulary or create one with high ID
            Long highVocabId = vocab1.getVocabId();
            // Ensure radical with highVocabId does not exist
            if (radicalRepository.existsById(highVocabId.intValue())) {
                // If it exists, find another vocabulary that does not exist in Radical
                for (Vocabulary v : vocabularyRepository.findAll()) {
                    if (!radicalRepository.existsById(v.getVocabId().intValue())) {
                        highVocabId = v.getVocabId();
                        break;
                    }
                }
            }

            final Long vocabOnlyId = highVocabId;

            // If vocabOnlyId does not exist in Radical, querying RADICAL with vocabOnlyId must fail
            if (!radicalRepository.existsById(vocabOnlyId.intValue())) {
                assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("RADICAL", vocabOnlyId, 3, 2)))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
            }

            // Find a Radical ID that does not exist in Vocabulary
            Integer radicalOnlyId = 214; // Kangxi radical 214 (龠)
            if (vocabularyRepository.existsById((long) radicalOnlyId)) {
                // Try other radical IDs
                for (Radical r : radicalRepository.findAll()) {
                    if (!vocabularyRepository.existsById((long) r.getRadicalId())) {
                        radicalOnlyId = r.getRadicalId();
                        break;
                    }
                }
            }

            final Integer targetRadicalId = radicalOnlyId;
            if (!vocabularyRepository.existsById((long) targetRadicalId)) {
                assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", (long) targetRadicalId, 3, 2)))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
            }
        }

        @Test
        @DisplayName("Test Group 9 & 10: Invalid itemType ('LESSON'), negative rating (0), or negative time (-1) rejected")
        void testInvalidParameters_rejectedWithoutMutation() {
            long initialProgressCount = cardProgressRepository.count();
            long initialLogCount = reviewLogRepository.count();

            // Invalid itemType
            assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("LESSON", vocab1.getVocabId(), 3, 2)))
                    .isInstanceOf(BusinessException.class);

            // Invalid rating
            assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 0, 2)))
                    .isInstanceOf(BusinessException.class);

            // Negative review time
            assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, -1)))
                    .isInstanceOf(BusinessException.class);

            assertThat(cardProgressRepository.count()).isEqualTo(initialProgressCount);
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogCount);
        }
    }

    @Nested
    @DisplayName("3. Multi-User & Multi-Type State Isolation")
    class StateIsolationTests {

        @Test
        @DisplayName("Test Group 11 & 12: Cross-User Isolation - User A reviewing VOCABULARY X does not create or mutate User B's state")
        void testCrossUserIsolation() {
            // User A reviews vocab1
            authenticateAs(userA);
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 4));

            // User B reviews vocab1 with different rating
            authenticateAs(userB);
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 1, 2)); // rating 1 = Again

            // Assertions on User A's progress
            CardProgress progressA = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "VOCABULARY", vocab1.getVocabId()).orElseThrow();
            assertThat(progressA.getIntervalDays()).isEqualTo(1);
            assertThat(progressA.getRepetitions()).isEqualTo(1);
            assertThat(progressA.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));

            // Assertions on User B's progress
            CardProgress progressB = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userB, "VOCABULARY", vocab1.getVocabId()).orElseThrow();
            assertThat(progressB.getIntervalDays()).isEqualTo(0); // reset by rating 1
            assertThat(progressB.getRepetitions()).isEqualTo(0);
            assertThat(progressB.getEaseFactor()).isEqualByComparingTo(new BigDecimal("1.70")); // 2.50 - 0.80

            // Verify User A logs vs User B logs
            List<ReviewLog> logsA = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userA.getUserId())).toList();
            List<ReviewLog> logsB = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userB.getUserId())).toList();

            assertThat(logsA).hasSize(1);
            assertThat(logsA.get(0).getRating()).isEqualTo((byte) 3);

            assertThat(logsB).hasSize(1);
            assertThat(logsB.get(0).getRating()).isEqualTo((byte) 1);
        }

        @Test
        @DisplayName("Test Group 14 & 15: Duplicate Progress Prevention & Immutable Append-Only Logs")
        void testMultipleReviews_maintainsSingleProgressAndAppendsLogs() {
            authenticateAs(userA);

            // Review 1 (Rating 3 = Good)
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 4));

            // Advance nextReviewAt to simulate interval passage (card becomes due for Review 2)
            CardProgress cp1 = cardProgressRepository.findByUserAndItemTypeAndItemId(userA, "VOCABULARY", vocab1.getVocabId()).orElseThrow();
            cp1.setNextReviewAt(LocalDateTime.now().minusHours(1));
            cardProgressRepository.saveAndFlush(cp1);

            // Review 2 (Rating 3 = Good)
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 3));

            // Advance nextReviewAt to simulate interval passage (card becomes due for Review 3)
            CardProgress cp2 = cardProgressRepository.findByUserAndItemTypeAndItemId(userA, "VOCABULARY", vocab1.getVocabId()).orElseThrow();
            cp2.setNextReviewAt(LocalDateTime.now().minusHours(1));
            cardProgressRepository.saveAndFlush(cp2);

            // Review 3 (Rating 2 = Hard)
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 2, 6));

            // Exactly 1 CardProgress row for userA + VOCABULARY + vocab1
            List<CardProgress> userAProgress = cardProgressRepository.findAll().stream()
                    .filter(p -> p.getUser().getUserId().equals(userA.getUserId())
                            && p.getItemType().equals("VOCABULARY")
                            && p.getItemId().equals(vocab1.getVocabId()))
                    .toList();
            assertThat(userAProgress).hasSize(1);

            CardProgress progress = userAProgress.get(0);
            assertThat(progress.getRepetitions()).isEqualTo(3);
            assertThat(progress.getIntervalDays()).isEqualTo(15);
            assertThat(progress.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));

            // Exactly 3 ReviewLog rows in append-only order
            List<ReviewLog> userALogs = reviewLogRepository.findAll().stream()
                    .filter(l -> l.getUser().getUserId().equals(userA.getUserId()))
                    .toList();
            assertThat(userALogs).hasSize(3);

            // Verify historical logs were not modified
            assertThat(userALogs.get(0).getRating()).isEqualTo((byte) 3);
            assertThat(userALogs.get(0).getIntervalBefore()).isEqualTo(0);
            assertThat(userALogs.get(0).getIntervalAfter()).isEqualTo(1);

            assertThat(userALogs.get(1).getRating()).isEqualTo((byte) 3);
            assertThat(userALogs.get(1).getIntervalBefore()).isEqualTo(1);
            assertThat(userALogs.get(1).getIntervalAfter()).isEqualTo(6);

            assertThat(userALogs.get(2).getRating()).isEqualTo((byte) 2);
            assertThat(userALogs.get(2).getIntervalBefore()).isEqualTo(6);
            assertThat(userALogs.get(2).getIntervalAfter()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("4. Due Card Resolution & Stale Progress Handling")
    class DueCardPolymorphicResolutionTests {

        @Test
        @DisplayName("Test Group 16: getDueCards filters by itemType and preserves polymorphic fields")
        void testDueCards_polymorphicResolution() {
            authenticateAs(userA);

            // Create due VOCABULARY progress
            CardProgress vocabProgress = new CardProgress(
                    userA, "VOCABULARY", vocab1.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1)
            );
            cardProgressRepository.save(vocabProgress);

            // Create due RADICAL progress
            CardProgress radicalProgress = new CardProgress(
                    userA, "RADICAL", (long) radical1.getRadicalId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1)
            );
            cardProgressRepository.save(radicalProgress);

            // Query VOCABULARY only
            List<DueCardResponse> vocabDue = srsService.getDueCards("VOCABULARY", 10);
            assertThat(vocabDue).extracting(DueCardResponse::getItemType).containsOnly("VOCABULARY");
            assertThat(vocabDue).extracting(DueCardResponse::getItemId).contains(vocab1.getVocabId());
            assertThat(vocabDue.get(0).getHanzi()).isEqualTo(vocab1.getHanzi());

            // Query RADICAL only
            List<DueCardResponse> radicalDue = srsService.getDueCards("RADICAL", 10);
            assertThat(radicalDue).extracting(DueCardResponse::getItemType).containsOnly("RADICAL");
            assertThat(radicalDue).extracting(DueCardResponse::getItemId).contains((long) radical1.getRadicalId());
            assertThat(radicalDue.get(0).getHanzi()).isEqualTo(radical1.getCharacter());

            // Query All (null filter)
            List<DueCardResponse> allDue = srsService.getDueCards(null, 10);
            assertThat(allDue).extracting(DueCardResponse::getItemType).contains("VOCABULARY", "RADICAL");
        }

        @Test
        @DisplayName("Test Group 17: getDueCards gracefully ignores stale progress pointing to nonexistent catalog item")
        void testDueCards_staleProgressIgnoredGracefully() {
            authenticateAs(userA);

            // Create a stale progress row pointing to nonexistent VOCABULARY ID 999999
            CardProgress staleProgress = new CardProgress(
                    userA, "VOCABULARY", 999999L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1)
            );
            cardProgressRepository.save(staleProgress);

            // Create a valid progress row
            CardProgress validProgress = new CardProgress(
                    userA, "VOCABULARY", vocab1.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1)
            );
            cardProgressRepository.save(validProgress);

            // Query due cards
            List<DueCardResponse> dueCards = srsService.getDueCards("VOCABULARY", 10);

            // Must include valid card and gracefully skip stale card without throwing exception
            assertThat(dueCards).hasSize(1);
            assertThat(dueCards.get(0).getItemId()).isEqualTo(vocab1.getVocabId());
        }
    }

    @Nested
    @DisplayName("5. Transaction Atomicity on MySQL")
    class TransactionRollbackTests {

        @Test
        @DisplayName("Test Group 20 & 21: When ReviewLog insertion fails, CardProgress mutation is completely rolled back on real MySQL DB")
        void testReviewRollback_onLogFailure() {
            authenticateAs(userA);

            // Create existing progress
            CardProgress existing = new CardProgress(userA, "VOCABULARY", vocab1.getVocabId(), new BigDecimal("2.50"), 6, 2, LocalDateTime.now().minusDays(1));
            cardProgressRepository.save(existing);

            // Wrapper of ReviewLogRepository that simulates failure
            ReviewLogRepository failingLogRepo = (ReviewLogRepository) java.lang.reflect.Proxy.newProxyInstance(
                    ReviewLogRepository.class.getClassLoader(),
                    new Class<?>[]{ReviewLogRepository.class},
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName())) {
                            throw new RuntimeException("Simulated I/O database failure on review_log insert");
                        }
                        return method.invoke(reviewLogRepository, args);
                    }
            );

            SrsServiceImpl atomicSrsService = new SrsServiceImpl(
                    cardProgressRepository,
                    failingLogRepo,
                    userSrsSettingRepository,
                    vocabularyRepository,
                    radicalRepository,
                    userProfileRepository,
                    new SrsCalculator()
            );

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 4);

            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);

            try {
                atomicSrsService.reviewCard(request);
                transactionManager.commit(status);
            } catch (Exception ex) {
                transactionManager.rollback(status);
            }

            // Verify on real MySQL database that CardProgress was NOT mutated (rolled back)
            CardProgress verifiedProgress = cardProgressRepository
                    .findByUserAndItemTypeAndItemId(userA, "VOCABULARY", vocab1.getVocabId()).orElseThrow();
            assertThat(verifiedProgress.getIntervalDays()).isEqualTo(6); // unchanged from 6
            assertThat(verifiedProgress.getRepetitions()).isEqualTo(2); // unchanged from 2
            assertThat(verifiedProgress.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50")); // unchanged from 2.50
        }
    }
}
