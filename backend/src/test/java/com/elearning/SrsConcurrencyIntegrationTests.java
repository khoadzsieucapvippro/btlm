package com.elearning;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.SrsService;
import com.elearning.common.ErrorCode;
import com.elearning.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-threaded Concurrency Integration Tests for SRS CardProgress Optimistic Locking (BE-CONC-001).
 * Verifies on real MySQL 8.4 Testcontainers:
 * - Two concurrent reviewCard() requests on the same CardProgress row are prevented from lost updates.
 * - Exactly one transaction commits (version 0 -> 1); the stale concurrent transaction fails with OptimisticLockingFailureException.
 * - The failed transaction rolls back completely (no duplicate ReviewLog, no corrupted CardProgress state).
 * - Sequential reviews increment @Version monotonically (0 -> 1 -> 2 -> ...).
 */
@SpringBootTest
@DisplayName("Task 8D.1 / BE-CONC-001 & BE-CONC-002: SRS Multi-Threaded Concurrency Integration Tests")
class SrsConcurrencyIntegrationTests {

    @Autowired
    private SrsService srsService;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

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
    private com.elearning.repository.UserSrsSettingRepository userSrsSettingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UserProfile testLearner;
    private Vocabulary testVocab;
    private Lesson testLesson;
    private String userEmail;

    private Vocabulary createApprovedVocab(String hanzi, String pinyin, String pinyinRaw, String meaningHanViet, String meaningVi) {
        Vocabulary v = vocabularyRepository.save(new Vocabulary(hanzi, pinyin, pinyinRaw, meaningHanViet, meaningVi));
        if (testLesson != null) {
            lessonVocabularyRepository.save(new LessonVocabulary(testLesson, v, (int) lessonVocabularyRepository.count() + 1));
        }
        return v;
    }

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        userEmail = "learner_conc_" + uniqueSuffix + "@test.com";

        Account account = new Account();
        account.setEmailOrPhone(userEmail);
        account.setPasswordHash("passwordHash");
        account.setStatus("Active");
        if (learnerRole != null) {
            account.getRoles().add(learnerRole);
        }
        account = accountRepository.save(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setFullName("Learner Conc " + uniqueSuffix);
        testLearner = userProfileRepository.save(profile);

        testLesson = new Lesson();
        testLesson.setTitle("Conc Lesson " + uniqueSuffix);
        testLesson.setStatus("Approved");
        testLesson.setCreatedBy(account);
        testLesson = lessonRepository.save(testLesson);

        testVocab = createApprovedVocab("念" + uniqueSuffix, "niàn", "nian" + uniqueSuffix, "Niệm", "Ghi nhớ");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (testLesson != null) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
            lessonRepository.delete(testLesson);
        }
        if (testLearner != null) {
            userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
            reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
            cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, "VOCABULARY", LocalDateTime.now().plusYears(10)));
            userProfileRepository.delete(testLearner);
        }
        if (testVocab != null && testVocab.getVocabId() != null) {
            vocabularyRepository.delete(testVocab);
        }
    }

    @Test
    @DisplayName("GIVEN existing CardProgress WHEN two concurrent reviewCard() requests execute THEN one succeeds, one fails with OptimisticLockingFailureException, zero lost updates")
    void testConcurrentReviewCard_optimisticLockPreventsLostUpdate() throws InterruptedException, ExecutionException {
        // 1. Seed existing CardProgress with version = 0, repetitions = 1, interval = 1
        CardProgress initialProgress = new CardProgress(
                testLearner,
                "VOCABULARY",
                testVocab.getVocabId(),
                new BigDecimal("2.50"),
                1,
                1,
                LocalDateTime.now().minusHours(1)
        );
        initialProgress = cardProgressRepository.saveAndFlush(initialProgress);
        assertThat(initialProgress.getVersion()).isEqualTo(0L);

        long progressId = initialProgress.getProgressId();
        long initialLogCount = reviewLogRepository.count();

        // 2. Prepare 2 concurrent threads with synchronization barriers
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockConflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> reviewTask = () -> {
            // Set SecurityContext for this worker thread
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            readyLatch.countDown();
            // Wait for both threads to be ready before firing
            startLatch.await(5, TimeUnit.SECONDS);

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 5); // Rating 3 = Good
            try {
                DueCardResponse response = srsService.reviewCard(request);
                if (response != null) {
                    successCount.incrementAndGet();
                }
            } catch (OptimisticLockingFailureException ex) {
                optimisticLockConflictCount.incrementAndGet();
            } catch (BusinessException be) {
                if (be.getErrorCode() == ErrorCode.CONFLICT) {
                    optimisticLockConflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                // Check cause chain for optimistic locking
                Throwable cause = ex;
                boolean isOptimistic = false;
                while (cause != null) {
                    if (cause instanceof OptimisticLockingFailureException
                            || cause instanceof jakarta.persistence.OptimisticLockException
                            || cause instanceof org.hibernate.StaleObjectStateException) {
                        isOptimistic = true;
                        break;
                    }
                    cause = cause.getCause();
                }
                if (isOptimistic) {
                    optimisticLockConflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        // 3. Launch threads
        Future<Void> future1 = executor.submit(reviewTask);
        Future<Void> future2 = executor.submit(reviewTask);

        // Wait for both threads to reach start line
        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        // Release the trigger
        startLatch.countDown();

        future1.get();
        future2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 4. Verify outcomes: Exactly 1 success and 1 optimistic lock conflict
        assertThat(successCount.get())
                .as("Exactly one concurrent request must succeed")
                .isEqualTo(1);
        assertThat(optimisticLockConflictCount.get())
                .as("The concurrent stale request must encounter optimistic locking conflict")
                .isEqualTo(1);
        assertThat(otherErrorCount.get()).isZero();

        // 5. Verify database integrity
        CardProgress finalProgress = cardProgressRepository.findById(progressId).orElseThrow();
        assertThat(finalProgress.getVersion())
                .as("CardProgress version must be incremented to 1 by the single successful write")
                .isEqualTo(1L);
        assertThat(finalProgress.getRepetitions())
                .as("Repetitions must reflect exactly 1 increment (1 -> 2), NOT double incremented or corrupted")
                .isEqualTo(2);
        assertThat(finalProgress.getIntervalDays())
                .as("Interval must be updated according to SM-2 for rep=2 (interval=6)")
                .isEqualTo(6);

        // 6. Verify ReviewLog rollback: Failed transaction must NOT persist an orphaned ReviewLog
        long finalLogCount = reviewLogRepository.count();
        assertThat(finalLogCount - initialLogCount)
                .as("Exactly 1 ReviewLog must be persisted; the failed transaction must roll back its log entry")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN CardProgress WHEN sequential reviewCard() calls execute THEN @Version increments monotonically (0 -> 1 -> 2 -> 3)")
    void testSequentialReviews_versionIncrementsMonotonically() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 1. Initial review: creates new CardProgress (initial version 0)
        ReviewCardRequest req1 = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4);
        srsService.reviewCard(req1);

        CardProgress p1 = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
        assertThat(p1.getVersion()).isEqualTo(0L);
        assertThat(p1.getRepetitions()).isEqualTo(1);

        // Advance nextReviewAt to simulate interval passage (card becomes due for review 2)
        p1.setNextReviewAt(LocalDateTime.now().minusHours(1));
        cardProgressRepository.saveAndFlush(p1);

        // 2. Second review: updates CardProgress (version becomes 1)
        ReviewCardRequest req2 = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 3);
        srsService.reviewCard(req2);

        CardProgress p2 = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
        assertThat(p2.getVersion()).isEqualTo(2L);
        assertThat(p2.getRepetitions()).isEqualTo(2);

        // Advance nextReviewAt to simulate interval passage (card becomes due for review 3)
        p2.setNextReviewAt(LocalDateTime.now().minusHours(1));
        cardProgressRepository.saveAndFlush(p2);

        // 3. Third review: updates CardProgress (version becomes 4)
        ReviewCardRequest req3 = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 4, 2);
        srsService.reviewCard(req3);

        CardProgress p3 = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
        assertThat(p3.getVersion()).isEqualTo(4L);
        assertThat(p3.getRepetitions()).isEqualTo(3);
    }

    @Test
    @DisplayName("BE-CONC-002: GIVEN maxReviewPerDay=3 and 2 existing reviews WHEN two concurrent reviewCard() execute on different cards THEN exactly 1 succeeds, 1 rejected with CONFLICT, final count is 3")
    void testConcurrentReviewCard_nearDailyQuotaLimit_pessimisticLockEnforcesQuotaAtomicity() throws InterruptedException, ExecutionException {
        // 1. Create two distinct vocabularies for this learner to review
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocabA = createApprovedVocab("字" + suffix, "zì", "zi" + suffix, "Tự", "Chữ");
        Vocabulary vocabB = createApprovedVocab("词" + suffix, "cí", "ci" + suffix, "Từ", "Từ ngữ");

        // 2. Set daily quota to 3
        UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner)
                .orElseGet(() -> new UserSrsSetting(testLearner, 20, 3));
        setting.setMaxReviewPerDay(3);
        userSrsSettingRepository.saveAndFlush(setting);

        // 3. Pre-seed 2 reviews today (so current count = 2 = 3 - 1)
        ReviewLog log1 = new ReviewLog(testLearner, "VOCABULARY", 9991L, (byte) 3, 0, 1, 3);
        ReviewLog log2 = new ReviewLog(testLearner, "VOCABULARY", 9992L, (byte) 3, 0, 1, 3);
        reviewLogRepository.saveAllAndFlush(List.of(log1, log2));

        long startOfDayCount = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, java.time.LocalDate.now().atStartOfDay());
        assertThat(startOfDayCount).isEqualTo(2L);

        // 4. Launch 2 concurrent threads attempting to review vocabA and vocabB simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger quotaConflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> taskA = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 3, 4);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (com.elearning.exception.BusinessException ex) {
                if (ex.getErrorCode() == com.elearning.common.ErrorCode.CONFLICT) {
                    quotaConflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Callable<Void> taskB = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 4);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (com.elearning.exception.BusinessException ex) {
                if (ex.getErrorCode() == com.elearning.common.ErrorCode.CONFLICT) {
                    quotaConflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(taskA);
        Future<Void> f2 = executor.submit(taskB);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown(); // Fire both threads simultaneously

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 5. Assert: Exactly 1 succeeded and exactly 1 was rejected with CONFLICT
        assertThat(successCount.get())
                .as("Exactly one concurrent request must claim the final quota slot")
                .isEqualTo(1);
        assertThat(quotaConflictCount.get())
                .as("The other concurrent request must be rejected with 409 CONFLICT daily quota limit reached")
                .isEqualTo(1);
        assertThat(otherErrorCount.get()).isZero();

        // 6. Assert: Final review count today must be EXACTLY 3 (limit), never 4
        long finalCountToday = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, java.time.LocalDate.now().atStartOfDay());
        assertThat(finalCountToday)
                .as("Final committed ReviewLog count today must not exceed maxReviewPerDay (3)")
                .isEqualTo(3L);

        // Clean up vocabA and vocabB
        lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
        vocabularyRepository.delete(vocabA);
        vocabularyRepository.delete(vocabB);
    }

    @Test
    @DisplayName("BE-CONC-002: GIVEN currentCount == maxReviewPerDay WHEN reviewCard() called THEN rejected with CONFLICT and zero side effects")
    void testSequentialReview_atDailyQuotaLimit_rejectedImmediately() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Set quota to 1
        UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner)
                .orElseGet(() -> new UserSrsSetting(testLearner, 20, 1));
        setting.setMaxReviewPerDay(1);
        userSrsSettingRepository.saveAndFlush(setting);

        // 1st review: succeeds (count = 1)
        ReviewCardRequest req1 = new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4);
        srsService.reviewCard(req1);

        long countAfterFirst = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, java.time.LocalDate.now().atStartOfDay());
        assertThat(countAfterFirst).isEqualTo(1L);

        // 2nd review: quota reached (1/1) -> rejected with CONFLICT
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab2 = createApprovedVocab("书" + suffix, "shū", "shu" + suffix, "Thư", "Sách");
        ReviewCardRequest req2 = new ReviewCardRequest("VOCABULARY", vocab2.getVocabId(), 3, 4);

        org.junit.jupiter.api.Assertions.assertThrows(com.elearning.exception.BusinessException.class, () -> {
            srsService.reviewCard(req2);
        });

        // Verify count remains 1 and vocab2 has NO CardProgress
        long countAfterSecond = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, java.time.LocalDate.now().atStartOfDay());
        assertThat(countAfterSecond).isEqualTo(1L);
        assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab2.getVocabId())).isEmpty();

        lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
        vocabularyRepository.delete(vocab2);
    }

    @Test
    @DisplayName("BE-CONC-002: User A reaching quota does not block User B from reviewing")
    void testUserIsolation_userAReachedQuotaDoesNotBlockUserB() {
        // Create User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accountB = new Account();
        accountB.setEmailOrPhone("learner_b_" + suffixB + "@test.com");
        accountB.setPasswordHash("passwordHash");
        accountB.setStatus("Active");
        accountB = accountRepository.save(accountB);

        UserProfile userB = new UserProfile();
        userB.setAccount(accountB);
        userB.setFullName("Learner B " + suffixB);
        userB = userProfileRepository.save(userB);

        UserSrsSetting settingB = new UserSrsSetting(userB, 20, 10);
        userSrsSettingRepository.save(settingB);

        // Set User A quota to 1 and exhaust it
        UserSrsSetting settingA = userSrsSettingRepository.findByUser(testLearner)
                .orElseGet(() -> new UserSrsSetting(testLearner, 20, 1));
        settingA.setMaxReviewPerDay(1);
        userSrsSettingRepository.saveAndFlush(settingA);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
        );
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4));

        // User A is now exhausted (1/1)
        assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4)))
                .isInstanceOf(com.elearning.exception.BusinessException.class);

        // Switch to User B -> should succeed!
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("learner_b_" + suffixB + "@test.com", "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
        );
        DueCardResponse respB = srsService.reviewCard(new ReviewCardRequest("VOCABULARY", testVocab.getVocabId(), 3, 4));
        assertThat(respB).isNotNull();
        assertThat(respB.getItemType()).isEqualTo("VOCABULARY");

        // Clean up User B
        userSrsSettingRepository.delete(settingB);
        reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(userB));
        cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(userB, "VOCABULARY", LocalDateTime.now().plusYears(10)));
        userProfileRepository.delete(userB);
        accountRepository.delete(accountB);
    }

    @Test
    @DisplayName("GIVEN existing CardProgress WHEN interleaved review transactions overlap THEN optimistic locking causes second transaction to fail with OptimisticLockingFailureException and rollback its ReviewLog")
    void testInterleavedConcurrentReviewTransactions_cardProgressOptimisticLockPreventsLostUpdate() throws Exception {
        CardProgress initialProgress = new CardProgress(
                testLearner,
                "VOCABULARY",
                testVocab.getVocabId(),
                new BigDecimal("2.50"),
                1,
                1,
                LocalDateTime.now().minusHours(1)
        );
        initialProgress = cardProgressRepository.saveAndFlush(initialProgress);
        Long progressId = initialProgress.getProgressId();
        assertThat(initialProgress.getVersion()).isEqualTo(0L);

        long initialLogCount = reviewLogRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothReadLatch = new CountDownLatch(2);
        CountDownLatch tx1CommittedLatch = new CountDownLatch(1);

        AtomicInteger tx1Success = new AtomicInteger(0);
        AtomicInteger tx2Conflict = new AtomicInteger(0);

        // Thread 1: Review Transaction 1
        Future<Void> future1 = executor.submit(() -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.execute(status -> {
                CardProgress p1 = cardProgressRepository.findById(progressId).orElseThrow();
                assertThat(p1.getVersion()).isEqualTo(0L);

                bothReadLatch.countDown();
                try {
                    bothReadLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                p1.setRepetitions(2);
                p1.setIntervalDays(6);
                cardProgressRepository.saveAndFlush(p1);

                ReviewLog log1 = new ReviewLog(testLearner, "VOCABULARY", testVocab.getVocabId(), (byte) 3, 1, 6, 5);
                reviewLogRepository.saveAndFlush(log1);
                return null;
            });
            tx1Success.incrementAndGet();
            tx1CommittedLatch.countDown();
            return null;
        });

        // Thread 2: Review Transaction 2 (holding stale version 0)
        Future<Void> future2 = executor.submit(() -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            bothReadLatch.countDown();
            try {
                bothReadLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try {
                tt.execute(status -> {
                    CardProgress p2 = cardProgressRepository.findById(progressId).orElseThrow();
                    try {
                        tx1CommittedLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    p2.setRepetitions(2);
                    p2.setIntervalDays(6);
                    cardProgressRepository.saveAndFlush(p2);

                    ReviewLog log2 = new ReviewLog(testLearner, "VOCABULARY", testVocab.getVocabId(), (byte) 3, 1, 6, 5);
                    reviewLogRepository.saveAndFlush(log2);
                    return null;
                });
            } catch (Exception ex) {
                if (isOptimisticLockException(ex)) {
                    tx2Conflict.incrementAndGet();
                }
            }
            return null;
        });

        future1.get();
        future2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(tx1Success.get()).isEqualTo(1);
        assertThat(tx2Conflict.get()).as("Second transaction with stale version 0 must fail with OptimisticLockingFailureException").isEqualTo(1);

        CardProgress finalProgress = cardProgressRepository.findById(progressId).orElseThrow();
        assertThat(finalProgress.getVersion()).isEqualTo(1L);
        assertThat(finalProgress.getRepetitions()).isEqualTo(2);

        long finalLogCount = reviewLogRepository.count();
        assertThat(finalLogCount - initialLogCount)
                .as("Exactly 1 ReviewLog must be persisted; the losing transaction must roll back its log entry")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN unreviewed card WHEN two concurrent reviewCard() requests execute for the first time THEN database constraints and pessimistic lock ensure exactly one CardProgress is created without corruption")
    void testConcurrentFirstTimeReview_newCardInitialState_pessimisticLockPreventsDuplicateCardProgress() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary newVocab = createApprovedVocab("新" + suffix, "xīn", "xin" + suffix, "Tân", "Mới");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> reviewTask = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", newVocab.getVocabId(), 3, 5);
                DueCardResponse resp = srsService.reviewCard(req);
                if (resp != null) {
                    successCount.incrementAndGet();
                }
            } catch (com.elearning.exception.BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                if (isOptimisticLockException(ex) || isConstraintViolationException(ex)) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(reviewTask);
        Future<Void> f2 = executor.submit(reviewTask);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(otherErrorCount.get()).isZero();
        assertThat(successCount.get()).as("At least 1 review must succeed").isGreaterThanOrEqualTo(1);
        assertThat(successCount.get() + conflictCount.get()).isEqualTo(2);

        // Assert database integrity: exactly 1 CardProgress record for this (user, itemType, itemId)
        List<CardProgress> progressList = cardProgressRepository.findAll().stream()
                .filter(p -> p.getUser().getUserId().equals(testLearner.getUserId())
                        && "VOCABULARY".equals(p.getItemType())
                        && newVocab.getVocabId().equals(p.getItemId()))
                .toList();

        assertThat(progressList).as("Exactly one CardProgress must exist for this card (no duplicate rows)").hasSize(1);
        CardProgress p = progressList.get(0);
        assertThat(p.getRepetitions()).isEqualTo(successCount.get());

        List<ReviewLog> logs = reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(
                testLearner, "VOCABULARY", newVocab.getVocabId()
        );
        assertThat(logs).as("Number of ReviewLog records must exactly match successCount").hasSize(successCount.get());

        // Cleanup
        cardProgressRepository.delete(p);
        reviewLogRepository.deleteAll(logs);
        lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(testLesson.getLessonId()));
        vocabularyRepository.delete(newVocab);
    }

    private boolean isConstraintViolationException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.springframework.dao.DataIntegrityViolationException
                    || (cause.getMessage() != null && cause.getMessage().contains("Duplicate entry"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isOptimisticLockException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ConcurrencyFailureException
                    || cause instanceof OptimisticLockingFailureException
                    || cause instanceof jakarta.persistence.OptimisticLockException
                    || cause instanceof org.hibernate.StaleObjectStateException
                    || cause instanceof org.hibernate.dialect.lock.OptimisticEntityLockException
                    || cause instanceof org.hibernate.exception.LockAcquisitionException
                    || cause instanceof jakarta.persistence.RollbackException
                    || cause instanceof TransactionException
                    || (cause.getMessage() != null && (cause.getMessage().contains("stale") || cause.getMessage().contains("Deadlock")))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
