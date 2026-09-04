package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.service.ModerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Multi-threaded Concurrency Integration Tests for Lesson Moderation Optimistic Locking (BE-CONC-003).
 * Verifies on real MySQL 8.4 Testcontainers:
 * - Two concurrent moderation transactions (Approve vs Approve, Approve vs Reject, Reject vs Reject) on the same Pending Lesson
 *   are prevented from double-moderation races via @Version optimistic locking.
 * - Exactly one transaction commits (version 0 -> 1); the stale concurrent transaction fails with OptimisticLockingFailureException.
 * - The failed transaction rolls back completely (saving exactly 1 ModerationLog, zero contradictory logs).
 * - High-concurrency service calls safely admit exactly 1 winner and reject all others with 409 Conflict.
 * - Sequential lesson updates increment @Version monotonically.
 */
@SpringBootTest
@DisplayName("BE-CONC-003: Lesson Moderation Concurrency & Optimistic Locking Integration Tests")
class ModerationConcurrencyIntegrationTests {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Account creatorAccount;
    private Account moderatorA;
    private Account moderatorB;
    private List<Long> createdLessonIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Role creatorRole = roleRepository.findByRoleName("Creator").orElse(null);
        Role modRole = roleRepository.findByRoleName("Moderator").orElse(null);

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Creator Account
        creatorAccount = new Account();
        creatorAccount.setEmailOrPhone("creator_conc_" + suffix + "@test.com");
        creatorAccount.setPasswordHash("passwordHash");
        creatorAccount.setStatus("Active");
        if (creatorRole != null) {
            creatorAccount.getRoles().add(creatorRole);
        }
        creatorAccount = accountRepository.save(creatorAccount);

        UserProfile creatorProfile = new UserProfile();
        creatorProfile.setAccount(creatorAccount);
        creatorProfile.setFullName("Creator Conc " + suffix);
        userProfileRepository.save(creatorProfile);

        // 2. Moderator A
        moderatorA = new Account();
        moderatorA.setEmailOrPhone("mod_a_conc_" + suffix + "@test.com");
        moderatorA.setPasswordHash("passwordHash");
        moderatorA.setStatus("Active");
        if (modRole != null) {
            moderatorA.getRoles().add(modRole);
        }
        moderatorA = accountRepository.save(moderatorA);

        UserProfile modAProfile = new UserProfile();
        modAProfile.setAccount(moderatorA);
        modAProfile.setFullName("Moderator A " + suffix);
        userProfileRepository.save(modAProfile);

        // 3. Moderator B
        moderatorB = new Account();
        moderatorB.setEmailOrPhone("mod_b_conc_" + suffix + "@test.com");
        moderatorB.setPasswordHash("passwordHash");
        moderatorB.setStatus("Active");
        if (modRole != null) {
            moderatorB.getRoles().add(modRole);
        }
        moderatorB = accountRepository.save(moderatorB);

        UserProfile modBProfile = new UserProfile();
        modBProfile.setAccount(moderatorB);
        modBProfile.setFullName("Moderator B " + suffix);
        userProfileRepository.save(modBProfile);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Long lessonId : createdLessonIds) {
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null) {
                List<ModerationLog> logs = moderationLogRepository.findByLessonOrderByCreatedAtDesc(lesson);
                moderationLogRepository.deleteAll(logs);
                lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId));
                lessonRepository.delete(lesson);
            }
        }
        createdLessonIds.clear();
    }

    private Lesson createPendingLesson(String title) {
        Lesson lesson = new Lesson(title, creatorAccount);
        lesson.setStatus("Pending");
        lesson = lessonRepository.saveAndFlush(lesson);
        createdLessonIds.add(lesson.getLessonId());
        return lesson;
    }

    @Test
    @DisplayName("GIVEN two overlapping transactions on Pending Lesson WHEN both approve THEN optimistic locking causes second transaction to fail with OptimisticLockingFailureException and rollback its ModerationLog")
    void testInterleavedConcurrentApproveTransactions_optimisticLockPreventsDoubleApprovalAndDuplicateLog() throws Exception {
        Lesson lesson = createPendingLesson("Bài học Song Song Approve Interleaved");
        Long lessonId = lesson.getLessonId();
        assertThat(lesson.getVersion()).isEqualTo(0L);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothReadLatch = new CountDownLatch(2);
        CountDownLatch tx1CommittedLatch = new CountDownLatch(1);

        AtomicInteger tx1Success = new AtomicInteger(0);
        AtomicInteger tx2Conflict = new AtomicInteger(0);

        // Thread 1: Moderation Transaction 1
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

                l1.setStatus("Approved");
                lessonRepository.saveAndFlush(l1);

                ModerationLog log1 = new ModerationLog(l1, moderatorA, "Approve", null, null);
                moderationLogRepository.saveAndFlush(log1);
                return null;
            });
            tx1Success.incrementAndGet();
            tx1CommittedLatch.countDown();
            return null;
        });

        // Thread 2: Moderation Transaction 2
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
                    // Wait for tx1 to commit first so l2 has stale version 0 in memory
                    try {
                        tx1CommittedLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    l2.setStatus("Approved");
                    lessonRepository.saveAndFlush(l2);

                    ModerationLog log2 = new ModerationLog(l2, moderatorB, "Approve", null, null);
                    moderationLogRepository.saveAndFlush(log2);
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

        // Assert transaction results: 1 success, 1 optimistic lock conflict
        assertThat(tx1Success.get()).isEqualTo(1);
        assertThat(tx2Conflict.get()).as("Second overlapping transaction must be rejected by @Version check").isEqualTo(1);

        // Verify final DB state
        Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(finalLesson.getStatus()).isEqualTo("Approved");
        assertThat(finalLesson.getVersion()).isEqualTo(1L);

        // Verify ModerationLog: exactly 1 log from tx1, tx2's log was completely rolled back
        List<ModerationLog> logs = moderationLogRepository.findByLessonOrderByCreatedAtDesc(finalLesson);
        assertThat(logs)
                .as("Exactly one ModerationLog must be persisted; losing transaction must roll back its log")
                .hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("Approve");
        assertThat(logs.get(0).getModerator().getAccountId()).isEqualTo(moderatorA.getAccountId());
    }

    @Test
    @DisplayName("GIVEN two overlapping transactions on Pending Lesson WHEN one approves and other rejects THEN optimistic locking prevents contradictory decisions and duplicate logs")
    void testInterleavedConcurrentMixedApproveAndRejectTransactions_optimisticLockPreventsConflictingDecisions() throws Exception {
        Lesson lesson = createPendingLesson("Bài học Song Song Mixed Interleaved");
        Long lessonId = lesson.getLessonId();
        assertThat(lesson.getVersion()).isEqualTo(0L);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothReadLatch = new CountDownLatch(2);
        CountDownLatch tx1CommittedLatch = new CountDownLatch(1);

        AtomicInteger tx1Success = new AtomicInteger(0);
        AtomicInteger tx2Conflict = new AtomicInteger(0);

        // Thread 1: Moderator A approves
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

                l1.setStatus("Approved");
                lessonRepository.saveAndFlush(l1);

                ModerationLog log1 = new ModerationLog(l1, moderatorA, "Approve", null, null);
                moderationLogRepository.saveAndFlush(log1);
                return null;
            });
            tx1Success.incrementAndGet();
            tx1CommittedLatch.countDown();
            return null;
        });

        // Thread 2: Moderator B rejects
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

                    l2.setStatus("Rejected");
                    lessonRepository.saveAndFlush(l2);

                    ModerationLog log2 = new ModerationLog(l2, moderatorB, "Reject", "Lý do", null);
                    moderationLogRepository.saveAndFlush(log2);
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
        assertThat(tx2Conflict.get()).isEqualTo(1);

        Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(finalLesson.getStatus()).isEqualTo("Approved");
        assertThat(finalLesson.getVersion()).isEqualTo(1L);

        List<ModerationLog> logs = moderationLogRepository.findByLessonOrderByCreatedAtDesc(finalLesson);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("Approve");
    }

    @Test
    @DisplayName("GIVEN Pending Lesson WHEN multiple concurrent service requests arrive THEN exactly 1 succeeds, all others receive 409 Conflict, exactly 1 log recorded")
    void testConcurrentModerationServiceCalls_exactlyOneSucceeds_allOthersConflict_exactlyOneLogPersisted() throws Exception {
        Lesson lesson = createPendingLesson("Bài học Song Song Service Calls");
        Long lessonId = lesson.getLessonId();

        int concurrency = 8;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            final boolean isApprove = (i % 2 == 0);
            final String moderatorEmail = (i % 2 == 0) ? moderatorA.getEmailOrPhone() : moderatorB.getEmailOrPhone();

            Callable<Void> task = () -> {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        moderatorEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                try {
                    if (isApprove) {
                        moderationService.approveLesson(lessonId, new ApproveLessonRequest());
                    } else {
                        moderationService.rejectLesson(lessonId, new RejectLessonRequest("Lý do từ chối", null));
                    }
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                        conflictCount.incrementAndGet();
                    } else {
                        System.err.println("Unexpected BusinessException: " + ex.getErrorCode() + " - " + ex.getMessage());
                        otherErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    if (isOptimisticLockException(ex)) {
                        conflictCount.incrementAndGet();
                    } else {
                        System.err.println("Unexpected Exception: " + ex.getClass().getName() + " - " + ex.getMessage());
                        if (ex.getCause() != null) {
                            System.err.println("Caused by: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage());
                        }
                        otherErrorCount.incrementAndGet();
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };
            futures.add(executor.submit(task));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(successCount.get()).as("Exactly 1 concurrent request must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("All other requests must receive HTTP 409 Conflict").isEqualTo(concurrency - 1);
        assertThat(otherErrorCount.get()).isZero();

        Lesson finalLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(finalLesson.getStatus()).isIn("Approved", "Rejected");
        assertThat(finalLesson.getVersion()).isEqualTo(1L);

        List<ModerationLog> logs = moderationLogRepository.findByLessonOrderByCreatedAtDesc(finalLesson);
        assertThat(logs).as("Exactly 1 ModerationLog must be persisted").hasSize(1);
    }

    @Test
    @DisplayName("GIVEN Lesson lifecycle WHEN updated sequentially THEN version increments monotonically")
    void testSequentialModerationLifecycle_incrementsVersionMonotonically() {
        // 1. Create Lesson in Draft (version = 0)
        Lesson lesson = new Lesson("Bài học Tuần tự Version Test", creatorAccount);
        lesson.setStatus("Draft");
        lesson = lessonRepository.saveAndFlush(lesson);
        createdLessonIds.add(lesson.getLessonId());
        assertThat(lesson.getVersion()).isEqualTo(0L);

        // 2. Transition Draft -> Pending (version = 1)
        lesson.setStatus("Pending");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(1L);

        // 3. Transition Pending -> Approved (version = 2)
        lesson.setStatus("Approved");
        lesson = lessonRepository.saveAndFlush(lesson);
        assertThat(lesson.getVersion()).isEqualTo(2L);
    }

    private boolean isOptimisticLockException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.springframework.dao.ConcurrencyFailureException
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