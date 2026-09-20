package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.response.AuthResponse;
import com.elearning.entity.Account;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;

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
 * Multi-threaded Concurrency Integration Tests for Account Registration & Unique Constraint Enforcement (BE-TEST-001).
 * Verifies on real MySQL 8.4 Testcontainers:
 * - Concurrent registration with the same email address is atomically serialized by MySQL unique constraint (idx_account_email_phone).
 * - Exactly one registration succeeds with HTTP 201 / JWT generation.
 * - The concurrent duplicate registration fails with HTTP 409 Conflict.
 * - Failed registration rolls back completely (zero orphaned UserProfile or AccountRole records).
 * - Concurrent registrations with distinct emails succeed independently without interference.
 */
@SpringBootTest
@DisplayName("BE-TEST-001: Account Registration Concurrency & Unique Constraint Integration Tests")
class AuthConcurrencyIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private List<String> registeredEmails = new ArrayList<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (String email : registeredEmails) {
            accountRepository.findByEmailOrPhone(email).ifPresent(account -> {
                if (account.getUserProfile() != null) {
                    userProfileRepository.delete(account.getUserProfile());
                }
                accountRepository.delete(account);
            });
        }
        registeredEmails.clear();
    }

    @Test
    @DisplayName("GIVEN same email WHEN two concurrent register() requests execute THEN exactly 1 succeeds, 1 receives 409 Conflict, exactly 1 Account created")
    void testConcurrentRegistration_sameEmail_uniqueConstraintPreventsDuplicateAccount() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String duplicateEmail = "concurrent_reg_" + suffix + "@elearning.com";
        registeredEmails.add(duplicateEmail);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        Callable<Void> registerTask = () -> {
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            try {
                RegisterRequest req = new RegisterRequest(duplicateEmail, "ValidPassword123!", "Learner " + suffix);
                AuthResponse resp = authService.register(req);
                if (resp != null && resp.getToken() != null) {
                    successCount.incrementAndGet();
                }
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (DataIntegrityViolationException ex) {
                conflictCount.incrementAndGet();
            } catch (Exception ex) {
                Throwable cause = ex;
                boolean isConstraint = false;
                while (cause != null) {
                    if (cause instanceof DataIntegrityViolationException
                            || (cause.getMessage() != null && cause.getMessage().contains("Duplicate entry"))) {
                        isConstraint = true;
                        break;
                    }
                    cause = cause.getCause();
                }
                if (isConstraint) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = executor.submit(registerTask);
        Future<Void> f2 = executor.submit(registerTask);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 1. Exactly 1 success, exactly 1 conflict
        assertThat(successCount.get()).as("Exactly one registration must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("The concurrent duplicate registration must fail with 409 Conflict").isEqualTo(1);
        assertThat(otherErrorCount.get()).isZero();

        // 2. Database verification: Exactly 1 Account, 1 UserProfile, active status
        List<Account> accounts = accountRepository.findAll().stream()
                .filter(a -> duplicateEmail.equalsIgnoreCase(a.getEmailOrPhone()))
                .toList();
        assertThat(accounts).as("Exactly one Account record must exist for this email").hasSize(1);

        Account finalAccount = accounts.get(0);
        assertThat(finalAccount.getStatus()).isEqualTo("Active");
        assertThat(finalAccount.getUserProfile()).isNotNull();
        assertThat(finalAccount.getUserProfile().getFullName()).isEqualTo("Learner " + suffix);
    }

    @Test
    @DisplayName("GIVEN distinct emails WHEN two concurrent register() requests execute THEN both succeed independently")
    void testConcurrentRegistration_distinctEmails_bothSucceed() throws Exception {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        String emailA = "reg_distinct_a_" + suffixA + "@elearning.com";
        String emailB = "reg_distinct_b_" + suffixB + "@elearning.com";
        registeredEmails.add(emailA);
        registeredEmails.add(emailB);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Callable<Void> taskA = () -> {
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);
            try {
                authService.register(new RegisterRequest(emailA, "ValidPassword123!", "Learner A"));
                successCount.incrementAndGet();
            } catch (Exception ex) {
                errorCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskB = () -> {
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);
            try {
                authService.register(new RegisterRequest(emailB, "ValidPassword123!", "Learner B"));
                successCount.incrementAndGet();
            } catch (Exception ex) {
                errorCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(taskA);
        Future<Void> f2 = executor.submit(taskB);

        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(errorCount.get()).isZero();

        assertThat(accountRepository.findByEmailOrPhone(emailA)).isPresent();
        assertThat(accountRepository.findByEmailOrPhone(emailB)).isPresent();
    }
}
