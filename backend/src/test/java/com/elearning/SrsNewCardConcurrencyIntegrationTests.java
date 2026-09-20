package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.config.TimeConfig;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Concurrency integration tests on real MySQL Testcontainers for R1 Section 14 Critical Concurrency Test Matrix.
 * Verifies that concurrent introduction of new cards respects daily limits and prevents race conditions.
 */
@SpringBootTest
@DisplayName("R1 — Critical Concurrency Integration Tests (Section 14 Matrix)")
class SrsNewCardConcurrencyIntegrationTests {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE);

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
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;
    private UserProfile testLearner;
    private Lesson testLesson;
    private String learnerEmail;

    private final List<Vocabulary> createdVocabularies = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Lesson> createdLessons = new java.util.concurrent.CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        createdVocabularies.clear();
        createdLessons.clear();
        txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Account account = new Account();
            learnerEmail = "conc_learner_" + suffix + "@test.com";
            account.setEmailOrPhone(learnerEmail);
            account.setPasswordHash("hash123");
            account.setStatus("Active");
            if (learnerRole != null) {
                account.getRoles().add(learnerRole);
            }
            account = accountRepository.save(account);

            testLearner = new UserProfile();
            testLearner.setAccount(account);
            testLearner.setFullName("Conc Learner " + suffix);
            testLearner = userProfileRepository.save(testLearner);

            UserSrsSetting setting = new UserSrsSetting();
            setting.setUser(testLearner);
            setting.setNewCardsPerDay(10);
            setting.setMaxReviewPerDay(100);
            userSrsSettingRepository.save(setting);

            testLesson = new Lesson();
            testLesson.setTitle("NewCard Conc Approved Lesson " + suffix);
            testLesson.setStatus("Approved");
            testLesson.setCreatedBy(account);
            testLesson = lessonRepository.save(testLesson);
            createdLessons.add(testLesson);

            return null;
        });
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
            for (Lesson l : createdLessons) {
                lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(l.getLessonId()));
                lessonRepository.findById(l.getLessonId()).ifPresent(lessonRepository::delete);
            }
            if (testLearner != null) {
                reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
                cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, "VOCABULARY", java.time.LocalDateTime.now().plusYears(10)));
                userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
                userProfileRepository.delete(testLearner);
                if (testLearner.getAccount() != null) {
                    accountRepository.delete(testLearner.getAccount());
                }
            }
            if (!createdVocabularies.isEmpty()) {
                for (Vocabulary v : createdVocabularies) {
                    vocabularyRepository.findById(v.getVocabId()).ifPresent(vocabularyRepository::delete);
                }
            }
            return null;
        });
    }

    @Test
    @DisplayName("Case A: Two concurrent requests introduce the SAME vocabulary -> exactly 1 CardProgress created, no duplicate")
    void testCaseA_concurrentSameVocabularyIntroduction() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab = createApprovedVocab("字" + suffix, "zì", "zi_" + suffix, "Tự", "Chữ");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Callable<Void> task = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (Exception ex) {
                errorCount.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one CardProgress must exist for (testLearner, 'VOCABULARY', vocabId)
        List<CardProgress> progressList = txTemplate.execute(status ->
                cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())
                        .map(List::of).orElse(Collections.emptyList())
        );

        assertThat(progressList).hasSize(1);
        assertThat(progressList.get(0).getItemId()).isEqualTo(vocab.getVocabId());
    }

    @Test
    @DisplayName("Case B: newCardsPerDay=1, two concurrent different vocabulary introduction attempts -> exactly 1 succeeds, 1 rejected with CONFLICT")
    void testCaseB_concurrentDifferentVocabulary_quotaLimitOne() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab1 = createApprovedVocab("词" + suffix, "cí", "ci_" + suffix, "Từ", "Từ ngữ");
        Vocabulary vocab2 = createApprovedVocab("句" + suffix, "jù", "ju_" + suffix, "Cú", "Câu");

        // Set newCardsPerDay = 1
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(1);
            userSrsSettingRepository.saveAndFlush(setting);
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab1.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
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

        Callable<Void> task2 = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab2.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
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

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(otherErrorCount.get()).isZero();

        // Total introduced new cards today must be exactly 1
        long totalIntroducedToday = txTemplate.execute(status ->
                reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay())
        );
        assertThat(totalIntroducedToday).isEqualTo(1L);
    }

    @Test
    @DisplayName("Case C: newCardsPerDay=2, five concurrent requests for 5 different candidates -> exactly 2 succeed, 3 rejected")
    void testCaseC_fiveConcurrentRequests_quotaLimitTwo() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        List<Vocabulary> vocabList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            vocabList.add(createApprovedVocab("字" + i + suffix, "p" + i, "raw_" + i + "_" + suffix, "H" + i, "N" + i));
        }

        // Set newCardsPerDay = 2
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(2);
            userSrsSettingRepository.saveAndFlush(setting);
            return null;
        });

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            final Vocabulary v = vocabList.get(i);
            futures.add(executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                try {
                    ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", v.getVocabId(), 3, 2);
                    srsService.reviewCard(req);
                    successCount.incrementAndGet();
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

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(conflictCount.get()).isEqualTo(3);

        long totalIntroducedToday = txTemplate.execute(status ->
                reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay())
        );
        assertThat(totalIntroducedToday).isEqualTo(2L);
    }

    @Test
    @DisplayName("Case D: Same card belongs to two lessons, concurrent requests from two lesson contexts -> exactly 1 CardProgress created")
    void testCaseD_sameCardTwoLessonsConcurrent() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary sharedVocab = txTemplate.execute(status -> {
            Vocabulary v = vocabularyRepository.save(new Vocabulary("共" + suffix, "gòng", "gong_" + suffix, "Cộng", "Chung"));
            createdVocabularies.add(v);

            Lesson lessonA = lessonRepository.save(new Lesson("Lesson A " + suffix, testLearner.getAccount()));
            lessonA.setStatus("Approved");
            lessonA.addVocabulary(v, 1);
            lessonA = lessonRepository.save(lessonA);
            createdLessons.add(lessonA);

            Lesson lessonB = lessonRepository.save(new Lesson("Lesson B " + suffix, testLearner.getAccount()));
            lessonB.setStatus("Approved");
            lessonB.addVocabulary(v, 1);
            lessonB = lessonRepository.save(lessonB);
            createdLessons.add(lessonB);

            return v;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);

        Callable<Void> task = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", sharedVocab.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (Exception ignored) {
                // One thread wins, the other hits unique constraint/optimistic conflict
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<CardProgress> progressList = txTemplate.execute(status ->
                cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", sharedVocab.getVocabId())
                        .map(List::of).orElse(Collections.emptyList())
        );

        assertThat(progressList).hasSize(1);
    }

    @Test
    @DisplayName("Case E: Existing CardProgress, concurrent reviews do NOT consume new-card quota")
    void testCaseE_existingCardProgressDoesNotConsumeNewQuota() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab = createApprovedVocab("旧" + suffix, "jiù", "jiu_" + suffix, "Cựu", "Cũ");

        // 1. Initial first-review creates CardProgress
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
        );
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2));
        SecurityContextHolder.clearContext();

        // 2. Set newCardsPerDay = 1 and set card as due (so if existing card review consumed new quota, next new card would fail)
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(1);
            userSrsSettingRepository.saveAndFlush(setting);

            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            cp.setNextReviewAt(LocalDateTime.now().minusHours(1));
            cardProgressRepository.saveAndFlush(cp);
            return null;
        });

        // 3. Concurrent reviews on the existing card
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);

        Callable<Void> task = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (Exception ignored) {
                // Optimistic locking on the existing card is expected to fail the loser thread
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // New cards introduced count must remain exactly 1
        long totalIntroducedToday = txTemplate.execute(status ->
                reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay())
        );
        assertThat(totalIntroducedToday).isEqualTo(1L);
    }

    @Test
    @DisplayName("Adversarial Test 6: Zero-Log Concurrency - user has 0 logs, 0 progress, quota=1 -> exactly 1 succeeds, 1 rejected with 409 CONFLICT")
    void testAdversarial_zeroLogConcurrency_exactOneSuccessOneConflict() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocabA = createApprovedVocab("甲" + suffix, "jiǎ", "jia_" + suffix, "Giáp", "Thứ nhất");
        Vocabulary vocabB = createApprovedVocab("乙" + suffix, "yǐ", "yi_" + suffix, "Ất", "Thứ hai");

        // Ensure 0 logs, 0 progress, newCardsPerDay = 1
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(1);
            userSrsSettingRepository.saveAndFlush(setting);
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        Callable<Void> taskA = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
                } else {
                    otherCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherCount.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Callable<Void> taskB = () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 2);
                srsService.reviewCard(req);
                successCount.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
                } else {
                    otherCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherCount.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(taskA);
        Future<Void> f2 = executor.submit(taskB);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).as("Exactly 1 request must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("Exactly 1 request must be rejected with 409 CONFLICT").isEqualTo(1);
        assertThat(otherCount.get()).as("Zero other errors").isZero();

        // Database assertions: exactly 1 CardProgress and 1 ReviewLog
        txTemplate.execute(status -> {
            long progressCount = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(testLearner, java.time.LocalDateTime.now().plusYears(10));
            assertThat(progressCount).as("Database must contain exactly 1 CardProgress").isEqualTo(1L);

            long uniqueIntroduced = reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(uniqueIntroduced).as("Database must record exactly 1 new item introduced").isEqualTo(1L);
            return null;
        });
    }

    @Test
    @DisplayName("Adversarial Test 8: 20 concurrent reviews on the SAME vocabulary -> exactly 1 CardProgress created in DB")
    void testAdversarial_twentyConcurrentReviews_sameCard_dedupAndSingleProgress() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab = createApprovedVocab("同" + suffix, "tóng", "tong_" + suffix, "Đồng", "Cùng nhau");

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictOrOptimisticCount = new AtomicInteger(0);

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
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    conflictOrOptimisticCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Void> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Database assertions: exactly 1 CardProgress must exist in DB
        txTemplate.execute(status -> {
            List<CardProgress> list = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())
                    .map(List::of).orElse(Collections.emptyList());
            assertThat(list).as("Exactly 1 CardProgress record must exist in DB").hasSize(1);
            return null;
        });
    }

    @Test
    @DisplayName("Adversarial Test 9: Mixed 10 concurrent requests (A,A,B,B,C,C,D,D,E,E) with quota=2 -> unique new cards in DB <= 2")
    void testAdversarial_mixedTenConcurrentRequests_fiveCards_quotaTwo() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        List<Vocabulary> fiveVocabs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fiveVocabs.add(createApprovedVocab("混" + i + suffix, "hùn" + i, "hun_" + i + "_" + suffix, "Hỗn" + i, "Trộn" + i));
        }

        // Set newCardsPerDay = 2
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(2);
            userSrsSettingRepository.saveAndFlush(setting);
            return null;
        });

        // Prepare 10 tasks: 2 tasks for each of the 5 vocabularies (A, A, B, B, C, C, D, D, E, E)
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Vocabulary targetVocab = fiveVocabs.get(i / 2);
            futures.add(executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                try {
                    ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", targetVocab.getVocabId(), 3, 2);
                    srsService.reviewCard(req);
                } catch (Exception ignored) {
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Void> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Database assertions
        txTemplate.execute(status -> {
            long uniqueIntroduced = reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(uniqueIntroduced).as("Unique new cards introduced in DB must be exactly equal to quota (2)").isEqualTo(2L);

            for (Vocabulary v : fiveVocabs) {
                List<CardProgress> cpList = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", v.getVocabId())
                        .map(List::of).orElse(Collections.emptyList());
                assertThat(cpList.size()).as("Each vocabulary item must have at most 1 CardProgress").isLessThanOrEqualTo(1);
            }
            return null;
        });
    }

    @Test
    @DisplayName("Adversarial Test 14: Candidate retrieval (GET /srs/new-cards) is strictly read-only and idempotent")
    void testAdversarial_candidateRetrievalIsIdempotentAndReadOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Lesson lesson = txTemplate.execute(status -> {
            Lesson l = lessonRepository.save(new Lesson("Read Only Lesson " + suffix, testLearner.getAccount()));
            l.setStatus("Approved");
            for (int i = 1; i <= 3; i++) {
                Vocabulary v = vocabularyRepository.save(new Vocabulary("读" + i + suffix, "dú" + i, "du_" + i + "_" + suffix, "Độc" + i, "Đọc" + i));
                createdVocabularies.add(v);
                l.addVocabulary(v, i);
            }
            l = lessonRepository.save(l);
            createdLessons.add(l);
            return l;
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
        );

        // Call getNewCardCandidates 10 times in a row
        for (int i = 0; i < 10; i++) {
            var candidates = srsService.getNewCardCandidates(lesson.getLessonId(), 10);
            assertThat(candidates).hasSize(3);
        }
        SecurityContextHolder.clearContext();

        // Database assertions: ZERO CardProgress, ZERO ReviewLog, ZERO quota consumed
        txTemplate.execute(status -> {
            long progressCount = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(testLearner, java.time.LocalDateTime.now().plusYears(10));
            assertThat(progressCount).as("Zero CardProgress created by candidate preview").isZero();

            long logsCount = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(logsCount).as("Zero ReviewLog created by candidate preview").isZero();

            long newCardsCount = reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(newCardsCount).as("Zero new cards quota consumed").isZero();
            return null;
        });
    }

    @Test
    @DisplayName("Adversarial Test 12: Same-day Again relearning does not double-consume new-card quota")
    void testAdversarial_sameDayAgainRelearningDoesNotConsumeNewQuota() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab = createApprovedVocab("重" + suffix, "chóng", "chong_" + suffix, "Trọng", "Lặp lại");

        // Set newCardsPerDay = 1, maxReviewPerDay = 100
        txTemplate.execute(status -> {
            UserSrsSetting setting = userSrsSettingRepository.findByUser(testLearner).orElseThrow();
            setting.setNewCardsPerDay(1);
            setting.setMaxReviewPerDay(100);
            userSrsSettingRepository.saveAndFlush(setting);
            return null;
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
        );

        // 1. First review with Rating 1 (Again) -> consumes 1 new quota
        ReviewCardRequest reqAgain = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 1, 3);
        var res1 = srsService.reviewCard(reqAgain);
        assertThat(res1.getIntervalDays()).isEqualTo(0);
        assertThat(res1.getRepetitions()).isEqualTo(0);

        // 2. Re-review the same card 5 times today (4 times Again, 1 time Good)
        for (int i = 2; i <= 5; i++) {
            ReviewCardRequest reqRe = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 1, 2);
            var res = srsService.reviewCard(reqRe);
            assertThat(res.getIntervalDays()).isEqualTo(0);
        }
        // Final successful relearn review today with Good
        ReviewCardRequest reqFinal = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
        var resFinal = srsService.reviewCard(reqFinal);
        assertThat(resFinal.getIntervalDays()).isEqualTo(1);
        assertThat(resFinal.getRepetitions()).isEqualTo(1);

        SecurityContextHolder.clearContext();

        // Database assertions
        txTemplate.execute(status -> {
            long totalReviewsToday = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(testLearner, LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(totalReviewsToday).as("Total review logs today must be 6").isEqualTo(6L);

            long uniqueIntroducedToday = reviewLogRepository.countNewItemsIntroducedToday(testLearner, "VOCABULARY", LocalDate.now(BUSINESS_ZONE).atStartOfDay());
            assertThat(uniqueIntroducedToday).as("Unique new cards introduced today must be exactly 1").isEqualTo(1L);

            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(cp.getRepetitions()).as("Repetitions must reflect cumulative SM-2 reviews").isGreaterThan(0);
            return null;
        });
    }
}
