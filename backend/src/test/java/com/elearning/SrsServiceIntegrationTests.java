package com.elearning;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
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
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@SpringBootTest
@DisplayName("Task 7B.1: SrsService Real Database Integration Tests")
class SrsServiceIntegrationTests {

    @Autowired
    private SrsService srsService;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UserProfile learnerA;
    private UserProfile learnerB;
    private Vocabulary testVocab;
    private Radical testRadical;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        String uniqueSuffixA = UUID.randomUUID().toString().substring(0, 8);
        Account accountA = new Account();
        accountA.setEmailOrPhone("learner_a_" + uniqueSuffixA + "@test.com");
        accountA.setPasswordHash("passwordHash");
        accountA.setStatus("Active");
        if (learnerRole != null) {
            accountA.getRoles().add(learnerRole);
        }
        accountA = accountRepository.save(accountA);

        learnerA = new UserProfile();
        learnerA.setAccount(accountA);
        learnerA.setFullName("Learner A " + uniqueSuffixA);
        learnerA = userProfileRepository.save(learnerA);

        String uniqueSuffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accountB = new Account();
        accountB.setEmailOrPhone("learner_b_" + uniqueSuffixB + "@test.com");
        accountB.setPasswordHash("passwordHash");
        accountB.setStatus("Active");
        if (learnerRole != null) {
            accountB.getRoles().add(learnerRole);
        }
        accountB = accountRepository.save(accountB);

        learnerB = new UserProfile();
        learnerB.setAccount(accountB);
        learnerB.setFullName("Learner B " + uniqueSuffixB);
        learnerB = userProfileRepository.save(learnerB);

        testVocab = vocabularyRepository.findByHanziAndPinyinRaw("书", "shu")
                .orElseGet(() -> {
                    Vocabulary v = new Vocabulary("书", "shū", "shu", "Thư", "Sách");
                    v.setExampleSentence("看书");
                    v.setExampleTranslation("Đọc sách");
                    return vocabularyRepository.save(v);
                });

        testLesson = new Lesson();
        testLesson.setTitle("Service Test Lesson " + uniqueSuffixA);
        testLesson.setStatus("Approved");
        testLesson.setCreatedBy(accountA);
        testLesson = lessonRepository.save(testLesson);

        if (!lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(testLesson.getLessonId(), testVocab.getVocabId())) {
            lessonVocabularyRepository.save(new LessonVocabulary(testLesson, testVocab, 1));
        }

        testRadical = radicalRepository.findAll().stream().findFirst()
                .orElseGet(() -> radicalRepository.save(new Radical(1, "一", "yī", "Nhất", "Số một")));

        authenticateAs(learnerA.getAccount().getEmailOrPhone());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (testLesson != null) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
            lessonRepository.delete(testLesson);
        }
    }

    private void authenticateAs(String emailOrPhone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(emailOrPhone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
        );
    }

    @Nested
    @DisplayName("Review Flow & Real Persistence Tests")
    class ReviewFlowPersistenceTests {

        @Test
        @DisplayName("First review of vocabulary with Rating 3 (Good) creates CardProgress and ReviewLog in MySQL")
        void testReviewVocabulary_firstTime_good() {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4);

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response.getItemType()).isEqualTo("VOCABULARY");
            assertThat(response.getItemId()).isEqualTo(testVocab.getVocabId());
            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(response.getIntervalDays()).isEqualTo(1);
            assertThat(response.getRepetitions()).isEqualTo(1);

            // Direct DB verification
            Optional<CardProgress> cpOpt = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", testVocab.getVocabId());
            assertThat(cpOpt).isPresent();
            CardProgress cp = cpOpt.get();
            assertThat(cp.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));
            assertThat(cp.getIntervalDays()).isEqualTo(1);
            assertThat(cp.getRepetitions()).isEqualTo(1);
            assertThat(cp.getNextReviewAt()).isNotNull();

            List<ReviewLog> logs = reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(learnerA, "VOCABULARY", testVocab.getVocabId());
            assertThat(logs).hasSize(1);
            ReviewLog log = logs.get(0);
            assertThat(log.getRating()).isEqualTo((byte) 3);
            assertThat(log.getIntervalBefore()).isEqualTo(0);
            assertThat(log.getIntervalAfter()).isEqualTo(1);
            assertThat(log.getReviewTimeSeconds()).isEqualTo(4);
            assertThat(log.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("Multiple reviews update existing CardProgress row and append new ReviewLog rows")
        void testMultipleReviews_updatesProgressAndAppendsLogs() {
            // Review 1: Good (3) -> I: 0 -> 1, R: 1
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 3));

            // Fast forward nextReviewAt to simulate interval passage (card becomes due for Review 2)
            CardProgress cpAfterR1 = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            cpAfterR1.setNextReviewAt(LocalDateTime.now().minusHours(1));
            cardProgressRepository.saveAndFlush(cpAfterR1);

            // Review 2: Good (3) -> I: 1 -> 6, R: 2
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 2));

            // Fast forward nextReviewAt to simulate interval passage (card becomes due for Review 3)
            CardProgress cpAfterR2 = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            cpAfterR2.setNextReviewAt(LocalDateTime.now().minusHours(1));
            cardProgressRepository.saveAndFlush(cpAfterR2);

            // Review 3: Hard (2) -> I: 6 -> 15, R: 3, EF: 2.50 -> 2.36
            DueCardResponse r3 = srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 2, 5));

            assertThat(r3.getIntervalDays()).isEqualTo(15);
            assertThat(r3.getRepetitions()).isEqualTo(3);
            assertThat(r3.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));

            // Check single progress row
            List<CardProgress> allProgress = cardProgressRepository.findAll().stream()
                    .filter(p -> p.getUser().getUserId().equals(learnerA.getUserId()) && p.getItemId().equals(testVocab.getVocabId()))
                    .toList();
            assertThat(allProgress).hasSize(1);

            // Check 3 audit log rows
            List<ReviewLog> logs = reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(learnerA, "VOCABULARY", testVocab.getVocabId());
            assertThat(logs).hasSize(3);
            assertThat(logs.get(0).getIntervalBefore()).isEqualTo(6);
            assertThat(logs.get(0).getIntervalAfter()).isEqualTo(15);
            assertThat(logs.get(0).getRating()).isEqualTo((byte) 2);

            assertThat(logs.get(1).getIntervalBefore()).isEqualTo(1);
            assertThat(logs.get(1).getIntervalAfter()).isEqualTo(6);

            assertThat(logs.get(2).getIntervalBefore()).isEqualTo(0);
            assertThat(logs.get(2).getIntervalAfter()).isEqualTo(1);
        }

        @Test
        @DisplayName("Review of radical card persists correctly")
        void testReviewRadical_persistsCorrectly() {
            Long radicalIdLong = testRadical.getRadicalId().longValue();
            ReviewCardRequest request = new ReviewCardRequest("RADICAL", radicalIdLong, 4, 2);

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response.getItemType()).isEqualTo("RADICAL");
            assertThat(response.getItemId()).isEqualTo(radicalIdLong);
            assertThat(response.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.60"));
            assertThat(response.getIntervalDays()).isEqualTo(1);
            assertThat(response.getRepetitions()).isEqualTo(1);

            Optional<CardProgress> cpOpt = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "RADICAL", radicalIdLong);
            assertThat(cpOpt).isPresent();
        }
    }

    @Nested
    @DisplayName("User & Item Type Isolation Tests")
    class IsolationTests {

        @Test
        @DisplayName("User A and User B have completely isolated CardProgress and ReviewLogs on same item")
        void testCrossUserIsolation() {
            // Learner A reviews testVocab
            authenticateAs(learnerA.getAccount().getEmailOrPhone());
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 3));

            // Learner B reviews same testVocab with Again (1)
            authenticateAs(learnerB.getAccount().getEmailOrPhone());
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 1, 6));

            // Verify Learner A's progress
            CardProgress cpA = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            assertThat(cpA.getIntervalDays()).isEqualTo(1);
            assertThat(cpA.getRepetitions()).isEqualTo(1);

            // Verify Learner B's progress
            CardProgress cpB = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerB, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            assertThat(cpB.getIntervalDays()).isEqualTo(0);
            assertThat(cpB.getRepetitions()).isEqualTo(0);

            // Check review log isolation
            List<ReviewLog> logsA = reviewLogRepository.findByUserOrderByReviewedAtDesc(learnerA);
            List<ReviewLog> logsB = reviewLogRepository.findByUserOrderByReviewedAtDesc(learnerB);

            assertThat(logsA).hasSize(1);
            assertThat(logsA.get(0).getRating()).isEqualTo((byte) 3);

            assertThat(logsB).hasSize(1);
            assertThat(logsB.get(0).getRating()).isEqualTo((byte) 1);
        }

        @Test
        @DisplayName("Same item ID with VOCABULARY and RADICAL are stored as separate progress identities")
        void testItemTypeIsolation() {
            Long commonId = 999999L;

            // Create vocabulary and radical with specific ID or reuse existing
            CardProgress cpVocab = new CardProgress(learnerA, "VOCABULARY", commonId, new BigDecimal("2.50"), 1, 1, LocalDateTime.now());
            CardProgress cpRadical = new CardProgress(learnerA, "RADICAL", commonId, new BigDecimal("2.36"), 6, 2, LocalDateTime.now());

            cardProgressRepository.save(cpVocab);
            cardProgressRepository.save(cpRadical);

            Optional<CardProgress> foundVocab = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", commonId);
            Optional<CardProgress> foundRadical = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "RADICAL", commonId);

            assertThat(foundVocab).isPresent();
            assertThat(foundRadical).isPresent();
            assertThat(foundVocab.get().getProgressId()).isNotEqualTo(foundRadical.get().getProgressId());
        }
    }

    @Nested
    @DisplayName("Due Cards & Daily Limits Tests")
    class DueCardsAndDailyLimitsTests {

        @Test
        @DisplayName("getDueCards returns cards due when next_review_at <= now")
        void testGetDueCards_returnsDue() {
            CardProgress dueCard = new CardProgress(learnerA, "VOCABULARY", testVocab.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(2));
            cardProgressRepository.save(dueCard);

            List<DueCardResponse> dueList = srsService.getDueCards(null, 10);

            assertThat(dueList).isNotEmpty();
            assertThat(dueList).anyMatch(c -> c.getItemId().equals(testVocab.getVocabId()) && "VOCABULARY".equals(c.getItemType()));
        }

        @Test
        @DisplayName("getDueCards respects maxReviewPerDay setting from USER_SRS_SETTING")
        void testGetDueCards_respectsSetting() {
            userSrsSettingRepository.save(new UserSrsSetting(learnerA, 20, 1)); // limit = 1 review/day

            // Complete 1 review today
            reviewLogRepository.save(new ReviewLog(learnerA, "VOCABULARY", testVocab.getVocabId(), (byte) 3, 0, 1, 2));

            // Create a due card
            cardProgressRepository.save(new CardProgress(learnerA, "VOCABULARY", testVocab.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1)));

            List<DueCardResponse> dueList = srsService.getDueCards(null, 10);

            // Quota exhausted (1/1 reviews completed today) -> empty list
            assertThat(dueList).isEmpty();
        }

        @Test
        @DisplayName("getStudyStats returns accurate counts from real database")
        void testGetStudyStats() {
            userSrsSettingRepository.save(new UserSrsSetting(learnerA, 15, 80));

            cardProgressRepository.save(new CardProgress(learnerA, "VOCABULARY", testVocab.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(5)));
            reviewLogRepository.save(new ReviewLog(learnerA, "VOCABULARY", testVocab.getVocabId(), (byte) 3, 0, 1, 4));

            StudyStatsResponse stats = srsService.getStudyStats();

            assertThat(stats.getCardsDue()).isGreaterThanOrEqualTo(1L);
            assertThat(stats.getReviewsToday()).isGreaterThanOrEqualTo(1L);
            assertThat(stats.getNewCardsLimit()).isEqualTo(15);
            assertThat(stats.getMaxReviewLimit()).isEqualTo(80);
        }
    }

    @Nested
    @DisplayName("Transaction Atomicity & Rollback Tests")
    class TransactionAtomicityTests {

        @Test
        @DisplayName("When reviewLogRepository fails during reviewCard, CardProgress updates rollback cleanly on real MySQL DB")
        void testTransactionRollback_whenReviewLogFails() {
            // Create existing progress
            CardProgress existing = new CardProgress(learnerA, "VOCABULARY", testVocab.getVocabId(), new BigDecimal("2.50"), 6, 2, LocalDateTime.now().minusDays(1));
            cardProgressRepository.save(existing);

            // Create wrapper of reviewLogRepository that throws RuntimeException on save
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

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4);

            // Execute transaction with failure injection
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
            CardProgress verifiedProgress = cardProgressRepository.findByUserAndItemTypeAndItemId(learnerA, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            assertThat(verifiedProgress.getIntervalDays()).isEqualTo(6); // unchanged from 6
            assertThat(verifiedProgress.getRepetitions()).isEqualTo(2); // unchanged from 2
            assertThat(verifiedProgress.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50")); // unchanged from 2.50
        }
    }
}
