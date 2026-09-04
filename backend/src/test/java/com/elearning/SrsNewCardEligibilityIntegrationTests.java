package com.elearning;

import com.elearning.common.ErrorCode;
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
 * Real Database Integration Tests for R2.1: New Card Eligibility Enforcement.
 * Verifies on MySQL 8.4 Testcontainers:
 * 1. New Vocabulary in Approved Lesson -> PASS.
 * 2. New Vocabulary ONLY in Draft Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations.
 * 3. New Vocabulary ONLY in Rejected Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations.
 * 4. New Vocabulary ONLY in Pending Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations.
 * 5. New Vocabulary with NO Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations.
 * 6. New Vocabulary in Approved + Draft Lessons -> PASS.
 * 7. New Vocabulary in Multiple Approved Lessons -> PASS (exactly 1 CardProgress & 1 Quota).
 * 8. Existing CardProgress preserved when Lesson status changes to Draft/Rejected.
 * 9. New Radical card bypasses lesson eligibility check.
 * 10. Concurrency: Lesson status transition during review.
 */
@SpringBootTest
@DisplayName("Task R2.1: New Card Eligibility Enforcement Integration Tests")
public class SrsNewCardEligibilityIntegrationTests {

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
    private TransactionTemplate txTemplate;

    private UserProfile testLearner;
    private UserProfile testCreator;
    private Account testCreatorAccount;
    private String learnerEmail;

    private final List<Vocabulary> createdVocabularies = new ArrayList<>();
    private final List<Long> createdLessonIds = new ArrayList<>();
    private final List<Radical> createdRadicals = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
        Role creatorRole = roleRepository.findByRoleName("Creator").orElse(null);

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        learnerEmail = "learner_elig_" + uniqueSuffix + "@test.com";
        String creatorEmail = "creator_elig_" + uniqueSuffix + "@test.com";

        // Create learner account & profile
        Account learnerAccount = new Account();
        learnerAccount.setEmailOrPhone(learnerEmail);
        learnerAccount.setPasswordHash("passwordHash");
        learnerAccount.setStatus("Active");
        if (learnerRole != null) {
            learnerAccount.getRoles().add(learnerRole);
        }
        learnerAccount = accountRepository.save(learnerAccount);

        UserProfile lProfile = new UserProfile();
        lProfile.setAccount(learnerAccount);
        lProfile.setFullName("Learner Elig " + uniqueSuffix);
        testLearner = userProfileRepository.save(lProfile);

        // Create creator account & profile
        Account creatorAccount = new Account();
        creatorAccount.setEmailOrPhone(creatorEmail);
        creatorAccount.setPasswordHash("passwordHash");
        creatorAccount.setStatus("Active");
        if (creatorRole != null) {
            creatorAccount.getRoles().add(creatorRole);
        }
        testCreatorAccount = accountRepository.save(creatorAccount);

        UserProfile cProfile = new UserProfile();
        cProfile.setAccount(testCreatorAccount);
        cProfile.setFullName("Creator Elig " + uniqueSuffix);
        testCreator = userProfileRepository.save(cProfile);

        // Create learner SRS setting (default: 20 new / 100 review)
        UserSrsSetting setting = new UserSrsSetting(testLearner, 20, 100);
        userSrsSettingRepository.save(setting);

        // Authenticate as learner
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        // Clean up join tables and progresses
        if (testLearner != null) {
            reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
            cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, "VOCABULARY", LocalDateTime.now().plusYears(10)));
            cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, "RADICAL", LocalDateTime.now().plusYears(10)));
            userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
            userProfileRepository.delete(testLearner);
        }
        if (testCreator != null) {
            userProfileRepository.delete(testCreator);
        }

        for (Long lId : createdLessonIds) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lId));
            lessonRepository.findById(lId).ifPresent(lessonRepository::delete);
        }
        for (Vocabulary v : createdVocabularies) {
            vocabularyRepository.delete(v);
        }
        for (Radical r : createdRadicals) {
            radicalRepository.delete(r);
        }
    }

    private Vocabulary createVocabulary(String hanzi, String pinyin, String meaningVi) {
        return txTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary v = new Vocabulary(hanzi + suffix, pinyin, pinyin + suffix, "Han Viet", meaningVi);
            v = vocabularyRepository.save(v);
            createdVocabularies.add(v);
            return v;
        });
    }

    private Lesson createLesson(String title, String status) {
        return txTemplate.execute(statusTx -> {
            Lesson lesson = new Lesson();
            lesson.setTitle(title + " " + UUID.randomUUID().toString().substring(0, 6));
            lesson.setStatus(status);
            lesson.setCreatedBy(testCreatorAccount);
            lesson = lessonRepository.save(lesson);
            createdLessonIds.add(lesson.getLessonId());
            return lesson;
        });
    }

    private void linkVocabularyToLesson(Lesson lesson, Vocabulary vocabulary, int orderIndex) {
        txTemplate.execute(status -> {
            LessonVocabulary lv = new LessonVocabulary(lesson, vocabulary, orderIndex);
            lessonVocabularyRepository.save(lv);
            return null;
        });
    }

    @Nested
    @DisplayName("1. Single Lesson Status Scenarios")
    class SingleLessonStatusTests {

        @Test
        @DisplayName("Scenario 1: Vocabulary in Approved Lesson -> First review SUCCEEDS")
        void testNewVocabulary_inApprovedLesson_succeeds() {
            Vocabulary vocab = createVocabulary("Xue", "xue", "Hoc");
            Lesson lesson = createLesson("Lesson Approved", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res).isNotNull();
            assertThat(res.getItemId()).isEqualTo(vocab.getVocabId());
            assertThat(res.getItemType()).isEqualTo("VOCABULARY");
            assertThat(res.getRepetitions()).isEqualTo(1);
            assertThat(res.getIntervalDays()).isEqualTo(1);

            // DB assertions
            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(cp.getRepetitions()).isEqualTo(1);
            assertThat(cp.getIntervalDays()).isEqualTo(1);
        }

        @Test
        @DisplayName("Scenario 2: Vocabulary ONLY in Draft Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations")
        void testNewVocabulary_onlyInDraftLesson_rejected() {
            Vocabulary vocab = createVocabulary("Cao", "cao", "Co");
            Lesson lesson = createLesson("Lesson Draft", "Draft");
            linkVocabularyToLesson(lesson, vocab, 1);

            long initialLogs = reviewLogRepository.count();
            long initialProgress = cardProgressRepository.count();

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                        assertThat(be.getMessage()).isNotNull();
                    });

            // Invariant: zero DB mutation & quota unchanged
            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogs);
            assertThat(cardProgressRepository.count()).isEqualTo(initialProgress);

            StudyStatsResponse stats = srsService.getStudyStats();
            assertThat(stats.getReviewsToday()).isEqualTo(0);
        }

        @Test
        @DisplayName("Scenario 3: Vocabulary ONLY in Rejected Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations")
        void testNewVocabulary_onlyInRejectedLesson_rejected() {
            Vocabulary vocab = createVocabulary("Shu", "shu", "Cay");
            Lesson lesson = createLesson("Lesson Rejected", "Rejected");
            linkVocabularyToLesson(lesson, vocab, 1);

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                    });

            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
        }

        @Test
        @DisplayName("Scenario 4: Vocabulary ONLY in Pending Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations")
        void testNewVocabulary_onlyInPendingLesson_rejected() {
            Vocabulary vocab = createVocabulary("Hua", "hua", "Hoa");
            Lesson lesson = createLesson("Lesson Pending", "Pending");
            linkVocabularyToLesson(lesson, vocab, 1);

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                    });

            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
        }

        @Test
        @DisplayName("Scenario 5: Vocabulary with NO Lesson -> REJECT (422 UNPROCESSABLE_ENTITY), 0 DB mutations")
        void testNewVocabulary_noLessonAttached_rejected() {
            Vocabulary orphanVocab = createVocabulary("Gu", "gu", "Co doc");

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", orphanVocab.getVocabId(), 3, 3);
            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                    });

            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", orphanVocab.getVocabId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("2. Multi-Lesson Scenarios")
    class MultiLessonTests {

        @Test
        @DisplayName("Scenario 6: Approved + Draft Lessons -> First review ALLOWED")
        void testNewVocabulary_approvedAndDraftLessons_succeeds() {
            Vocabulary vocab = createVocabulary("Qiao", "qiao", "Cau");
            Lesson approvedLesson = createLesson("Lesson Approved", "Approved");
            Lesson draftLesson = createLesson("Lesson Draft", "Draft");
            linkVocabularyToLesson(approvedLesson, vocab, 1);
            linkVocabularyToLesson(draftLesson, vocab, 2);

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res).isNotNull();
            assertThat(res.getItemId()).isEqualTo(vocab.getVocabId());
            assertThat(res.getRepetitions()).isEqualTo(1);

            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(cp.getRepetitions()).isEqualTo(1);
        }

        @Test
        @DisplayName("Scenario 7: Multiple Approved Lessons -> First review ALLOWED (exactly 1 CardProgress)")
        void testNewVocabulary_multipleApprovedLessons_succeedsOnce() {
            Vocabulary vocab = createVocabulary("Jiang", "jiang", "Song");
            Lesson app1 = createLesson("Lesson App 1", "Approved");
            Lesson app2 = createLesson("Lesson App 2", "Approved");
            linkVocabularyToLesson(app1, vocab, 1);
            linkVocabularyToLesson(app2, vocab, 3);

            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res).isNotNull();

            // Verify exactly 1 CardProgress exists
            List<CardProgress> progresses = cardProgressRepository.findAll().stream()
                    .filter(cp -> cp.getUser().getUserId().equals(testLearner.getUserId())
                            && "VOCABULARY".equals(cp.getItemType())
                            && cp.getItemId().equals(vocab.getVocabId()))
                    .toList();
            assertThat(progresses).hasSize(1);
        }
    }

    @Nested
    @DisplayName("3. Lifecycle & Non-Vocabulary Scenarios")
    class LifecycleAndRadicalTests {

        @Test
        @DisplayName("Scenario 8: Existing CardProgress is PRESERVED when Lesson later becomes Draft/Rejected")
        void testExistingCardProgress_preservedWhenLessonStatusChanges() {
            Vocabulary vocab = createVocabulary("Lu", "lu", "Duong");
            Lesson lesson = createLesson("Lesson Approved Initially", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            // 1. Initial review while lesson is Approved -> creates CardProgress
            ReviewCardRequest req1 = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            srsService.reviewCard(req1);

            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(cp.getRepetitions()).isEqualTo(1);

            // 2. Lesson status changes to Rejected
            txTemplate.execute(status -> {
                Lesson l = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
                l.setStatus("Rejected");
                lessonRepository.saveAndFlush(l);
                return null;
            });

            // 3. Fast-forward nextReviewAt to past (card becomes due)
            txTemplate.execute(status -> {
                CardProgress p = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
                p.setNextReviewAt(LocalDateTime.now().minusHours(1));
                cardProgressRepository.saveAndFlush(p);
                return null;
            });

            // 4. Review existing card -> MUST BE ALLOWED (does not delete or block existing CardProgress)
            ReviewCardRequest req2 = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 3);
            DueCardResponse res2 = srsService.reviewCard(req2);

            assertThat(res2).isNotNull();
            assertThat(res2.getRepetitions()).isEqualTo(2);

            CardProgress updatedCp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(updatedCp.getRepetitions()).isEqualTo(2);
        }

        @Test
        @DisplayName("Scenario 9: New RADICAL card bypasses lesson eligibility check")
        void testNewRadical_bypassesLessonEligibility() {
            Radical rad = txTemplate.execute(status -> {
                int radId = 190 + (int) (Math.random() * 20);
                Radical r = new Radical(radId, "Biao", "biao", "Buu", "Toc dai");
                if (radicalRepository.existsById(radId)) {
                    return radicalRepository.findById(radId).get();
                }
                r = radicalRepository.save(r);
                createdRadicals.add(r);
                return r;
            });

            ReviewCardRequest req = new ReviewCardRequest("RADICAL", (long) rad.getRadicalId(), 3, 3);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res).isNotNull();
            assertThat(res.getItemType()).isEqualTo("RADICAL");
            assertThat(res.getItemId()).isEqualTo((long) rad.getRadicalId());
            assertThat(res.getRepetitions()).isEqualTo(1);

            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "RADICAL", (long) rad.getRadicalId()).orElseThrow();
            assertThat(cp.getRepetitions()).isEqualTo(1);
        }
    }

    private boolean isDeadlockException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.springframework.dao.CannotAcquireLockException
                    || cause instanceof org.hibernate.exception.LockAcquisitionException
                    || cause instanceof jakarta.persistence.PessimisticLockException
                    || (cause.getMessage() != null && cause.getMessage().contains("Deadlock"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Nested
    @DisplayName("4. Concurrency on Lesson Status Transition (R2.1A Moderation Race Verification)")
    class ConcurrencyTests {

        @Test
        @DisplayName("Order A: Review transaction commits first -> Lesson is later rejected -> Review succeeds and CardProgress is preserved")
        void testConcurrentModeration_OrderA_ReviewWinsBeforeRejection() {
            Vocabulary vocab = createVocabulary("Feng", "feng", "Gio");
            Lesson lesson = createLesson("Lesson Order A", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            // Step 1: Learner reviews new card while lesson is Approved
            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            DueCardResponse res = srsService.reviewCard(req);

            assertThat(res).isNotNull();
            assertThat(res.getRepetitions()).isEqualTo(1);
            assertThat(res.getIntervalDays()).isEqualTo(1);

            // Verify CardProgress and ReviewLog exist in DB
            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
            assertThat(cp.getRepetitions()).isEqualTo(1);

            // Step 2: Moderation transaction rejects the lesson
            txTemplate.execute(status -> {
                Lesson l = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
                l.setStatus("Rejected");
                lessonRepository.saveAndFlush(l);
                return null;
            });

            // Assert: Lesson is now Rejected, but existing CardProgress is preserved and reviewable when due
            Lesson updatedLesson = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
            assertThat(updatedLesson.getStatus()).isEqualTo("Rejected");

            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isPresent();
            assertThat(reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(testLearner, "VOCABULARY", vocab.getVocabId())).hasSize(1);
        }

        @Test
        @DisplayName("Order B: Moderation rejects lesson first -> Review transaction attempted -> Rejected with 422 and ZERO side effects")
        void testConcurrentModeration_OrderB_RejectionWinsBeforeReview() {
            Vocabulary vocab = createVocabulary("Shui", "shui", "Nuoc");
            Lesson lesson = createLesson("Lesson Order B", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            long initialProgressCount = cardProgressRepository.count();
            long initialLogCount = reviewLogRepository.count();

            // Step 1: Moderation transaction rejects the lesson
            txTemplate.execute(status -> {
                Lesson l = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
                l.setStatus("Rejected");
                lessonRepository.saveAndFlush(l);
                return null;
            });

            // Step 2: Learner attempts to review the new vocabulary card
            ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
            assertThatThrownBy(() -> srsService.reviewCard(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                        assertThat(be.getMessage()).contains("Từ vựng chưa thuộc bất kỳ bài học nào đã được phê duyệt để học mới");
                    });

            // Invariant assertions: 0 CardProgress, 0 ReviewLog, 0 quota consumed
            assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
            assertThat(cardProgressRepository.count()).isEqualTo(initialProgressCount);
            assertThat(reviewLogRepository.count()).isEqualTo(initialLogCount);
            assertThat(srsService.getStudyStats().getReviewsToday()).isZero();
        }

        @Test
        @DisplayName("Order C: True overlapping concurrent transactions (Review vs Moderation Rejection) barrier coordinated")
        void testConcurrentModeration_OrderC_TrueOverlappingTransactions() throws Exception {
            Vocabulary vocab = createVocabulary("Huo", "huo", "Lua");
            Lesson lesson = createLesson("Lesson Order C", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch reviewReadyLatch = new CountDownLatch(1);
            CountDownLatch moderationReadyLatch = new CountDownLatch(1);

            AtomicInteger reviewSuccessCount = new AtomicInteger(0);
            AtomicInteger review422Count = new AtomicInteger(0);
            AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

            // Thread 1: Learner Review
            Callable<Void> reviewTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                reviewReadyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                try {
                    ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
                    srsService.reviewCard(req);
                    reviewSuccessCount.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.UNPROCESSABLE_ENTITY) {
                        review422Count.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            // Thread 2: Moderator Rejection
            Callable<Void> moderationTask = () -> {
                moderationReadyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                txTemplate.execute(status -> {
                    Lesson l = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
                    l.setStatus("Rejected");
                    lessonRepository.saveAndFlush(l);
                    return null;
                });
                return null;
            };

            Future<Void> fReview = executor.submit(reviewTask);
            Future<Void> fMod = executor.submit(moderationTask);

            reviewReadyLatch.await(5, TimeUnit.SECONDS);
            moderationReadyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown(); // Release both threads simultaneously

            fReview.get(10, TimeUnit.SECONDS);
            fMod.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(unexpectedErrorCount.get()).as("Zero unexpected errors during concurrency race").isZero();

            // Under MySQL 8.4 REPEATABLE READ:
            // Either Review acquired its MVCC read snapshot when Lesson was Approved -> reviewSuccessCount = 1
            // Or Moderation committed before Review acquired its read view -> review422Count = 1
            int totalProcessed = reviewSuccessCount.get() + review422Count.get();
            assertThat(totalProcessed).isEqualTo(1);

            // Database consistency assertions
            Lesson finalLesson = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
            assertThat(finalLesson.getStatus()).isEqualTo("Rejected");

            if (reviewSuccessCount.get() == 1) {
                // If review won: exactly 1 CardProgress and 1 ReviewLog
                CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId()).orElseThrow();
                assertThat(cp.getRepetitions()).isEqualTo(1);
                assertThat(reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(testLearner, "VOCABULARY", vocab.getVocabId())).hasSize(1);
            } else {
                // If moderation won: 0 CardProgress and 0 ReviewLog
                assertThat(cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
                assertThat(reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(testLearner, "VOCABULARY", vocab.getVocabId())).isEmpty();
            }
        }

        @Test
        @DisplayName("Case 5: Multiple concurrent learner reviews while lesson status is rejected concurrently")
        void testConcurrentModeration_MultipleLearnersConcurrentReviewWhileLessonRejected() throws Exception {
            Vocabulary vocab = createVocabulary("Shan", "shan", "Nui");
            Lesson lesson = createLesson("Lesson Multi Concurrent", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            int learnerCount = 4;
            List<UserProfile> learners = new ArrayList<>();
            List<String> learnerEmails = new ArrayList<>();

            txTemplate.execute(status -> {
                Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
                for (int i = 0; i < learnerCount; i++) {
                    String email = "multi_learner_" + i + "_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
                    Account acc = new Account();
                    acc.setEmailOrPhone(email);
                    acc.setPasswordHash("hash123");
                    acc.setStatus("Active");
                    if (learnerRole != null) {
                        acc.getRoles().add(learnerRole);
                    }
                    acc = accountRepository.save(acc);

                    UserProfile p = new UserProfile();
                    p.setAccount(acc);
                    p.setFullName("Multi Learner " + i);
                    p = userProfileRepository.save(p);
                    learners.add(p);
                    learnerEmails.add(email);

                    UserSrsSetting s = new UserSrsSetting(p, 10, 50);
                    userSrsSettingRepository.save(s);
                }
                return null;
            });

            ExecutorService executor = Executors.newFixedThreadPool(learnerCount + 1);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch readyLatch = new CountDownLatch(learnerCount + 1);

            AtomicInteger reviewSuccess = new AtomicInteger(0);
            AtomicInteger reviewRejected422 = new AtomicInteger(0);
            AtomicInteger deadlockCount = new AtomicInteger(0);
            AtomicInteger unexpectedErrors = new AtomicInteger(0);

            List<Future<Void>> futures = new ArrayList<>();

            // Submit learner review tasks
            for (int i = 0; i < learnerCount; i++) {
                final String email = learnerEmails.get(i);
                futures.add(executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                    );
                    readyLatch.countDown();
                    startLatch.await(5, TimeUnit.SECONDS);

                    try {
                        ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2);
                        srsService.reviewCard(req);
                        reviewSuccess.incrementAndGet();
                    } catch (BusinessException be) {
                        if (be.getErrorCode() == ErrorCode.UNPROCESSABLE_ENTITY) {
                            reviewRejected422.incrementAndGet();
                        } else {
                            unexpectedErrors.incrementAndGet();
                        }
                    } catch (Throwable ex) {
                        if (isDeadlockException(ex)) {
                            deadlockCount.incrementAndGet();
                        } else {
                            unexpectedErrors.incrementAndGet();
                        }
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }

            // Submit Moderator rejection task
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                txTemplate.execute(status -> {
                    Lesson l = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
                    l.setStatus("Rejected");
                    lessonRepository.saveAndFlush(l);
                    return null;
                });
                return null;
            }));

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            for (Future<Void> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();

            assertThat(unexpectedErrors.get()).as("Zero unexpected exceptions").isZero();
            assertThat(reviewSuccess.get() + reviewRejected422.get() + deadlockCount.get()).isEqualTo(learnerCount);

            // Cleanup multi learners
            txTemplate.execute(status -> {
                for (UserProfile p : learners) {
                    reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(p));
                    cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(p, "VOCABULARY", LocalDateTime.now().plusYears(10)));
                    userSrsSettingRepository.findByUser(p).ifPresent(userSrsSettingRepository::delete);
                    userProfileRepository.delete(p);
                    if (p.getAccount() != null) {
                        accountRepository.delete(p.getAccount());
                    }
                }
                return null;
            });
        }
    }
}
