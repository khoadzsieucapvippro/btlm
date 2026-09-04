package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.CreatorLessonService;
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

/**
 * Multi-threaded Concurrency Integration Tests for Creator Lesson Lifecycle State Transitions (BE-TEST-001).
 * Verifies on real MySQL 8.4 Testcontainers:
 * - Concurrent submitForModeration() calls on the same Draft lesson are protected by @Version optimistic locking.
 * - Exactly one transaction commits (Draft -> Pending, version 0 -> 1); stale concurrent transaction fails with 409 Conflict.
 * - Interleaved updateMyLesson() vs submitForModeration() transactions prevent contradictory state transitions.
 * - Concurrent deleteMyLesson() vs submitForModeration() preserves database integrity and prevents dangling state.
 * - Monotonic @Version progression across creator lifecycle operations.
 */
@SpringBootTest
@DisplayName("BE-TEST-001: Creator Lesson Lifecycle Concurrency & Optimistic Locking Integration Tests")
class CreatorLessonConcurrencyIntegrationTests {

    @Autowired
    private CreatorLessonService creatorLessonService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Account creatorAccount;
    private Vocabulary testVocab;
    private List<Long> createdLessonIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Role creatorRole = roleRepository.findByRoleName("Creator").orElse(null);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        creatorAccount = new Account();
        creatorAccount.setEmailOrPhone("creator_life_conc_" + suffix + "@test.com");
        creatorAccount.setPasswordHash("passwordHash");
        creatorAccount.setStatus("Active");
        if (creatorRole != null) {
            creatorAccount.getRoles().add(creatorRole);
        }
        creatorAccount = accountRepository.save(creatorAccount);

        UserProfile creatorProfile = new UserProfile();
        creatorProfile.setAccount(creatorAccount);
        creatorProfile.setFullName("Creator Life Conc " + suffix);
        userProfileRepository.save(creatorProfile);

        testVocab = new Vocabulary("创" + suffix, "chuàng", "chuang" + suffix, "Sáng", "Sáng tạo");
        testVocab = vocabularyRepository.save(testVocab);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Long lessonId : createdLessonIds) {
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null) {
                lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId));
                lessonRepository.delete(lesson);
            }
        }
        createdLessonIds.clear();

        if (testVocab != null && testVocab.getVocabId() != null) {
            vocabularyRepository.delete(testVocab);
        }
    }

    private Lesson createDraftLessonWithVocab(String title) {
        Lesson lesson = new Lesson(title, creatorAccount);
        lesson.setStatus("Draft");
        lesson = lessonRepository.saveAndFlush(lesson);
        createdLessonIds.add(lesson.getLessonId());

        LessonVocabulary lv = new LessonVocabulary(lesson, testVocab, 1);
        lessonVocabularyRepository.saveAndFlush(lv);

        return lesson;
    }

    @Test
    @DisplayName("GIVEN Draft Lesson with vocabulary WHEN two concurrent submitForModeration() execute THEN exactly 1 succeeds, 1 receives 409 Conflict, status is Pending and version is 1")
    void testConcurrentSubmitForModeration_optimisticLockPreventsDoubleSubmission() throws Exception {
        Lesson lesson = createDraftLessonWithVocab("Bài học Concurrent Submit Test");
        Long lessonId = lesson.getLessonId();
        assertThat(lesson.getVersion()).isEqualTo(0L);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> submitTask = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    creatorAccount.getEmailOrPhone(), "password", List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                creatorLessonService.submitForModeration(lessonId);
                successCount.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                if (isOptimisticLockException(ex)) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(submitTask);
        Future<Void> f2 = executor.submit(submitTask);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(successCount.get()).as("Exactly 1 submit request must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("The second concurrent submit must fail with 409 Conflict / OptimisticLock").isEqualTo(1);
        assertThat(otherErrorCount.get()).isZero();

        Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(finalLesson.getStatus()).isEqualTo("Pending");
        assertThat(finalLesson.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GIVEN Draft Lesson WHEN interleaved Update and Submit transactions overlap THEN optimistic locking rejects stale submit transaction")
    void testInterleavedConcurrentUpdateAndSubmit_optimisticLockPreventsConflict() throws Exception {
        Lesson lesson = createDraftLessonWithVocab("Bài học Interleaved Update vs Submit");
        Long lessonId = lesson.getLessonId();
        assertThat(lesson.getVersion()).isEqualTo(0L);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothReadLatch = new CountDownLatch(2);
        CountDownLatch tx1CommittedLatch = new CountDownLatch(1);

        AtomicInteger tx1Success = new AtomicInteger(0);
        AtomicInteger tx2Conflict = new AtomicInteger(0);

        // Thread 1: Update Title
        Future<Void> future1 = executor.submit(() -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.execute(status -> {
                Lesson l1 = lessonRepository.findById(lessonId).orElseThrow();
                assertThat(l1.getVersion()).isEqualTo(0L);

                bothReadLatch.countDown();
                try {
                    bothReadLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                l1.setTitle("Bài học Tiêu Đề Đã Sửa");
                lessonRepository.saveAndFlush(l1);
                return null;
            });
            tx1Success.incrementAndGet();
            tx1CommittedLatch.countDown();
            return null;
        });

        // Thread 2: Submit for Review (holding stale version 0)
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
                    Lesson l2 = lessonRepository.findById(lessonId).orElseThrow();
                    try {
                        tx1CommittedLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    l2.setStatus("Pending");
                    lessonRepository.saveAndFlush(l2);
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

        Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(finalLesson.getTitle()).isEqualTo("Bài học Tiêu Đề Đã Sửa");
        assertThat(finalLesson.getStatus()).isEqualTo("Draft");
        assertThat(finalLesson.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GIVEN Draft Lesson WHEN concurrent Delete and Submit execute THEN system state remains completely consistent")
    void testConcurrentDeleteAndSubmit_preventsInconsistentState() throws Exception {
        Lesson lesson = createDraftLessonWithVocab("Bài học Concurrent Delete vs Submit");
        Long lessonId = lesson.getLessonId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger submitSuccess = new AtomicInteger(0);
        AtomicInteger deleteSuccess = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> submitTask = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    creatorAccount.getEmailOrPhone(), "password", List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                creatorLessonService.submitForModeration(lessonId);
                submitSuccess.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT || ex.getErrorCode() == ErrorCode.NOT_FOUND) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                if (isOptimisticLockException(ex)) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Callable<Void> deleteTask = () -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    creatorAccount.getEmailOrPhone(), "password", List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                creatorLessonService.deleteMyLesson(lessonId);
                deleteSuccess.incrementAndGet();
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT || ex.getErrorCode() == ErrorCode.NOT_FOUND) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                if (isOptimisticLockException(ex)) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(submitTask);
        Future<Void> f2 = executor.submit(deleteTask);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(otherErrorCount.get()).isZero();
        int totalSuccess = submitSuccess.get() + deleteSuccess.get();
        assertThat(totalSuccess).as("At least one operation succeeds and neither corrupts database").isGreaterThanOrEqualTo(1);

        // Verify database state: Either lesson is deleted or lesson is Pending
        boolean exists = lessonRepository.existsById(lessonId);
        if (exists) {
            Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(finalLesson.getStatus()).isEqualTo("Pending");
            assertThat(finalLesson.getVersion()).isEqualTo(1L);
        } else {
            assertThat(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId)).isEmpty();
        }
    }

    @Test
    @DisplayName("GIVEN Lesson lifecycle WHEN Creator updates sequentially THEN @Version increments monotonically")
    void testSequentialCreatorTransitions_versionIncrementsMonotonically() {
        // 1. Create Lesson in Draft (version = 0)
        Lesson lesson = new Lesson("Bài học Tuần tự Version Creator Test", creatorAccount);
        lesson.setStatus("Draft");
        lesson = lessonRepository.saveAndFlush(lesson);
        createdLessonIds.add(lesson.getLessonId());
        assertThat(lesson.getVersion()).isEqualTo(0L);

        // 2. Creator updates title (version 0 -> 1)
        lesson.setTitle("Tiêu đề lần 1");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(1L);

        // 3. Creator submits for review: Draft -> Pending (version 1 -> 2)
        lesson.setStatus("Pending");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(2L);

        // 4. Moderator rejects: Pending -> Rejected (version 2 -> 3)
        lesson.setStatus("Rejected");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(3L);

        // 5. Creator re-submits: Rejected -> Pending (version 3 -> 4)
        lesson.setStatus("Pending");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(4L);
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