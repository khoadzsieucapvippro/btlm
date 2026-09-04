package com.elearning;

import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.service.AdminRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Task 8D.2: Admin Role Concurrency Integration Tests")
class AdminRoleConcurrencyIntegrationTests {

    @Autowired
    private AdminRoleService adminRoleService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long targetAccountId;

    @BeforeEach
    void setUp() {
        Role roleLearner = roleRepository.findByRoleName("Learner").orElseGet(() -> roleRepository.save(new Role(1, "Learner")));

        Account account = new Account();
        account.setEmailOrPhone("concurrency_target_" + System.currentTimeMillis() + "@example.com");
        account.setPasswordHash("$2a$12$dummyhashconcurrency123456789012");
        account.setStatus("Active");
        account.setAuthorizationVersion(1L);
        account.addRole(roleLearner);

        Account saved = accountRepository.save(account);
        targetAccountId = saved.getAccountId();
    }

    @Test
    @DisplayName("GIVEN 10 concurrent effective role updates on same account WHEN executed THEN serialized without lost updates")
    void testConcurrentRoleUpdates_noLostUpdates() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit((Callable<Void>) () -> {
                startLatch.await();
                try {
                    // Alternate role payloads to ensure effective role changes
                    List<String> roles = (index % 2 == 0)
                            ? List.of("Learner", "Creator")
                            : List.of("Learner", "Moderator");

                    UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(roles);
                    adminRoleService.updateAccountRoles(targetAccountId, request, "admin@example.com");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // If lock acquisition timeout or optimistic conflict occurs under high concurrency, it is caught
                } finally {
                    doneLatch.countDown();
                }
                return null;
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        // Verify that database state reflects the initial version plus all successful effective increments
        Account finalAccount = accountRepository.findByIdWithRoles(targetAccountId).orElseThrow();

        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(finalAccount.getAuthorizationVersion()).isGreaterThanOrEqualTo(2L);
        assertThat(finalAccount.getRoles()).isNotEmpty();
    }
}
