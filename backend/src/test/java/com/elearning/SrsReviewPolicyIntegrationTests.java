package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real Database Integration Tests for R2: Review Policy & Due-Only Enforcement.
 * Verifies on MySQL 8.4 Testcontainers:
 * 1. Early Review on Future Card (nextReviewAt > now) is strictly rejected with 409 CONFLICT.
 * 2. Due Card (nextReviewAt <= now) is accepted and processed normally.
 * 3. Brand New Card (no CardProgress) is eligible for first-ever review.
 * 4. Same-day relearning from Again (interval=0, nextReviewAt=now) is immediately due and reviewable.
 * 5. Concurrent attempts to review a future card are safely rejected.
 */
@SpringBootTest
@DisplayName("R2: Review Policy & Due-Only Server Enforcement Integration Tests")
class SrsReviewPolicyIntegrationTests {

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
    private TransactionTemplate txTemplate;

    private UserProfile testLearner;
    private Lesson testLesson;
    private String learnerEmail;
    private final List<Vocabulary> createdVocabularies = new ArrayList<>();
    private final List<Radical> createdRadicals = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        learnerEmail = "r2_learner_" + suffix + "@test.com";

        txTemplate.execute(status -> {
            Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

            Account account = new Account();
            account.setEmailOrPhone(learnerEmail);
            account.setPasswordHash("hash123");
            account.setStatus("Active");
            if (learnerRole != null) {
                account.getRoles().add(learnerRole);
            }
            account = accountRepository.save(account);

            UserProfile profile = new UserProfile();
            profile.setAccount(account);
            profile.setFullName("R2 Learner " + suffix);
            testLearner = userProfileRepository.save(profile);

            UserSrsSetting setting = new UserSrsSetting(testLearner, 20, 100);
            userSrsSettingRepository.save(setting);

            testLesson = new Lesson();
            testLesson.setTitle("R2 Approved Lesson " + suffix);
            testLesson.setStatus("Approved");
            testLesson.setCreatedBy(account);
            testLesson = lessonRepository.save(testLesson);

            return null;
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
        );
    }

    private Vocabulary createApprovedVocab(String hanzi, String pinyin, String pinyinRaw, String meaningHanViet, String meaningVi) {
        return txTemplate.execute(status -> {
            Vocabulary v = vocabularyRepository.save(new Vocabulary(hanzi, pinyin, pinyinRaw, meaningHanViet, meaningVi));
            createdVocabularies.add(v);
            lessonVocabularyRepository.save(new LessonVocabulary(testLesson, v, createdVocabularies.size()));
            return v;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        txTemplate.execute(status -> {
            if (testLesson != null) {
                lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
                lessonRepository.delete(testLesson);
            }
            if (testLearner != null) {
                reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
                cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                        testLearner, "VOCABULARY", LocalDateTime.now().plusYears(10)));
                cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                        testLearner, "RADICAL", LocalDateTime.now().plusYears(10)));
                userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
                userProfileRepository.delete(testLearner);
                accountRepository.delete(testLearner.getAccount());
            }

            for (Vocabulary v : createdVocabularies) {
                vocabularyRepository.findById(v.getVocabId()).ifPresent(vocabularyRepository::delete);
            }
            createdVocabularies.clear();

            for (Radical r : createdRadicals) {
                radicalRepository.findById(r.getRadicalId()).ifPresent(radicalRepository::delete);
            }
            createdRadicals.clear();
            return null;
        });
    }

    @Nested
    @DisplayName("1. Early Review vs Due Card Policy Verification")
    class EarlyReviewPolicyTests {

        @Test
        @DisplayName("Case A: Due card with past nextReviewAt (< now) is accepted for review")
        void testDueCard_pastScheduledTime_accepted() {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = txTemplate.execute(status -> {
                Vocabulary v = vocabularyRepository.save(new Vocabulary("过" + suffix, "guò", "guo_" + suffix, "Quá", "Đã qua"));
                createdVocabularies.add(v);

                CardProgress cp = new CardProgress(testLearner, "VOCABULARY", v.getVocabId(),
                        new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(3));
                cardProgressRepository.save(cp);
                return v;
            });

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res.getItemId()).isEqualTo(vocab.getVocabId());
            assertThat(res.getIntervalDays()).isEqualTo(6);
            assertThat(res.getRepetitions()).isEqualTo(2);
        }

        @Test
        @DisplayName("Case B: Due card with exact current time (== now) is accepted for review")
        void testDueCard_exactNow_accepted() {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = txTemplate.execute(status -> {
                Vocabulary v = vocabularyRepository.save(new Vocabulary("当" + suffix, "dāng", "dang_" + suffix, "Đương", "Hiện tại"));
                createdVocabularies.add(v);

                CardProgress cp = new CardProgress(testLearner, "VOCABULARY", v.getVocabId(),
                        new BigDecimal("2.50"), 0, 0, LocalDateTime.now());
                cardProgressRepository.save(cp);
                return v;
            });

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res.getItemId()).isEqualTo(vocab.getVocabId());
            assertThat(res.getIntervalDays()).isEqualTo(1);
            assertThat(res.getRepetitions()).isEqualTo(1);
        }

        @Test
        @DisplayName("Case C: Future scheduled card (nextReviewAt > now) is strictly REJECTED with 409 CONFLICT")
        void testFutureCard_strictlyRejectedWithConflict() {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = txTemplate.execute(status -> {
                Vocabulary v = vocabularyRepository.save(new Vocabulary("未" + suffix, "wèi", "wei_" + suffix, "Vị", "Chưa đến"));
                createdVocabularies.add(v);

                CardProgress cp = new CardProgress(testLearner, "VOCABULARY", v.getVocabId(),
                        new BigDecimal("2.50"), 6, 2, LocalDateTime.now().plusDays(5));
                cardProgressRepository.save(cp);
                return v;
            });

            long initialLogCount = reviewLogRepository.count();

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);

            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Thẻ chưa đến hạn ôn tập");
                    });

            // Zero mutation in DB
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogCount);
            txTemplate.execute(status -> {
                CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
                assertThat(cp.getIntervalDays()).isEqualTo(6);
                assertThat(cp.getRepetitions()).isEqualTo(2);
                return null;
            });
        }
    }

    @Nested
    @DisplayName("2. Brand New Card Eligibility Tests")
    class NewCardEligibilityTests {

        @Test
        @DisplayName("Brand new card (no CardProgress) bypasses due-check and succeeds as first introduction")
        void testNewVocabulary_firstReview_succeeds() {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = createApprovedVocab("新" + suffix, "xīn", "xin_" + suffix, "Tân", "Mới");

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 4);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res.getItemId()).isEqualTo(vocab.getVocabId());
            assertThat(res.getIntervalDays()).isEqualTo(1);
            assertThat(res.getRepetitions()).isEqualTo(1);

            txTemplate.execute(status -> {
                CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
                assertThat(cp.getIntervalDays()).isEqualTo(1);
                assertThat(cp.getNextReviewAt()).isAfter(LocalDateTime.now());
                return null;
            });
        }

        @Test
        @DisplayName("Brand new radical (no CardProgress) bypasses due-check and succeeds as first introduction")
        void testNewRadical_firstReview_succeeds() {
            Radical rad = txTemplate.execute(status -> {
                int radId = 180 + (int) (Math.random() * 30);
                Radical r = radicalRepository.findById(radId)
                        .orElseGet(() -> radicalRepository.save(new Radical(radId, "音", "yīn", "Âm", "Âm thanh")));
                return r;
            });

            ReviewCardRequest req = new ReviewCardRequest("RADICAL", (long) rad.getRadicalId(), 4, 3);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res.getItemType()).isEqualTo("RADICAL");
            assertThat(res.getItemId()).isEqualTo((long) rad.getRadicalId());
            assertThat(res.getIntervalDays()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("3. Same-Day Relearning from Again (Rating 1)")
    class SameDayRelearningTests {

        @Test
        @DisplayName("Again (Rating 1) sets interval=0 and nextReviewAt=now, making card immediately eligible for same-day relearn")
        void testSameDayRelearn_immediateRelearningFlow() {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = createApprovedVocab("复" + suffix, "fù", "fu_" + suffix, "Phục", "Lặp lại");

            // Step 1: First review with Again (1) -> interval=0, nextReviewAt=now
            ReviewCardRequest reqAgain = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 1, 2);
            DueCardResponse r1 = srsService.reviewCard(reqAgain);
            assertThat(r1.getIntervalDays()).isEqualTo(0);
            assertThat(r1.getRepetitions()).isEqualTo(0);

            // Step 2: Immediate relearn review today with Good (3) -> accepted because nextReviewAt == now
            ReviewCardRequest reqRelearn = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            DueCardResponse r2 = srsService.reviewCard(reqRelearn);
            assertThat(r2.getIntervalDays()).isEqualTo(1);
            assertThat(r2.getRepetitions()).isEqualTo(1);

            // Step 3: Immediate third review is rejected with 409 CONFLICT because nextReviewAt is now tomorrow
            ReviewCardRequest reqThird = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            assertThatThrownBy(() -> srsService.reviewCard(reqThird))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("4. Concurrency on Future Card Rejection")
    class FutureCardConcurrencyTests {

        @Test
        @DisplayName("Concurrent review attempts on a future card are all rejected with 409 CONFLICT without corrupting logs")
        void testConcurrentReviewOnFutureCard_allRejected() throws Exception {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary vocab = txTemplate.execute(status -> {
                Vocabulary v = vocabularyRepository.save(new Vocabulary("锁" + suffix, "suǒ", "suo_" + suffix, "Tỏa", "Khóa"));
                createdVocabularies.add(v);

                CardProgress cp = new CardProgress(testLearner, "VOCABULARY", v.getVocabId(),
                        new BigDecimal("2.50"), 10, 3, LocalDateTime.now().plusDays(10));
                cardProgressRepository.save(cp);
                return v;
            });

            int numThreads = 4;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch readyLatch = new CountDownLatch(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);

            AtomicInteger conflictCount = new AtomicInteger(0);

            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                futures.add(executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                    );
                    readyLatch.countDown();
                    startLatch.await(5, TimeUnit.SECONDS);

                    try {
                        ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
                        srsService.reviewCard(req);
                    } catch (BusinessException ex) {
                        if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                            conflictCount.incrementAndGet();
                        }
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            for (Future<Void> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();

            assertThat(conflictCount.get()).as("All 4 concurrent threads must be rejected with 409 CONFLICT").isEqualTo(4);

            txTemplate.execute(status -> {
                List<ReviewLog> logs = reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(testLearner, "VOCABULARY", vocab.getVocabId());
                assertThat(logs).as("Zero review logs created for rejected future card").isEmpty();
                return null;
            });
        }
    }
}
