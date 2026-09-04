package com.elearning;

import com.elearning.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 8D.6: LoginRateLimiter Unit Tests")
class LoginRateLimiterTests {

    private MutableClock mutableClock;
    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        mutableClock = new MutableClock(Instant.parse("2026-08-31T12:00:00Z"), ZoneId.of("UTC"));
        // 5 max attempts within 60-second window
        rateLimiter = new LoginRateLimiter(5, 60, mutableClock);
    }

    @Test
    @DisplayName("GIVEN attempts below limit WHEN checking tryAcquire THEN all attempts succeed")
    void testAttemptsBelowLimitSucceed() {
        String ip = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
    }

    @Test
    @DisplayName("GIVEN attempts exceeding limit WHEN checking tryAcquire THEN subsequent attempts return false")
    void testAttemptsExceedingLimitBlocked() {
        String ip = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
        // 6th attempt is blocked
        assertThat(rateLimiter.tryAcquire(ip)).isFalse();
        // 7th attempt is also blocked
        assertThat(rateLimiter.tryAcquire(ip)).isFalse();
    }

    @Test
    @DisplayName("GIVEN distinct IPs WHEN checking tryAcquire THEN rate limits are isolated per IP")
    void testDifferentIpsIsolated() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip1)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(ip1)).isFalse();

        // ip2 should still be allowed
        assertThat(rateLimiter.tryAcquire(ip2)).isTrue();
    }

    @Test
    @DisplayName("GIVEN window expiration WHEN checking tryAcquire after window seconds THEN limit resets")
    void testWindowExpirationAllowsNewAttempts() {
        String ip = "192.168.1.50";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(ip)).isFalse();

        // Advance clock by 61 seconds (past the 60s sliding window)
        mutableClock.advanceSeconds(61);

        // Should now be permitted again
        assertThat(rateLimiter.tryAcquire(ip)).isTrue();
    }

    @Test
    @DisplayName("GIVEN manual reset WHEN called THEN attempt history is cleared")
    void testResetClearsHistory() {
        String ip = "192.168.1.20";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(ip)).isFalse();

        rateLimiter.reset();

        assertThat(rateLimiter.tryAcquire(ip)).isTrue();
    }

    // ========== R3.6: Memory Lifecycle / Key Eviction Tests ==========

    @Test
    @DisplayName("R3.6: GIVEN single attempt WHEN window expires THEN IP key is evicted from map")
    void testInactiveIpKeyIsEvictedAfterWindowExpires() {
        String ip = "192.168.1.100";
        // Single attempt
        assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1);

        // Advance clock past the window (61 seconds)
        mutableClock.advanceSeconds(61);

        // Proactive cleanup explicitly removes dead key
        rateLimiter.cleanupExpiredEntries();
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(0);

        // Next attempt starts fresh
        assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("R3.6: GIVEN many unique IPs WHEN all expire THEN memory is bounded and dead keys are evicted")
    void testManyUniqueIpsDoNotCauseUnboundedMemoryGrowth() {
        int uniqueIps = 1000;

        // Each IP makes one attempt
        for (int i = 0; i < uniqueIps; i++) {
            String ip = "10.0.0." + (i % 256) + "." + (i / 256);
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(uniqueIps);

        // Advance clock past window
        mutableClock.advanceSeconds(61);

        // Trigger opportunistic eviction by making one more request from a new IP
        String newIp = "192.168.0.1";
        assertThat(rateLimiter.tryAcquire(newIp)).isTrue();

        // Expired IPs must be pruned: only the single active new IP remains
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN capacity limit reached within window WHEN new key attempts acquire THEN rejected (fail-closed)")
    void testCapacitySaturationRejection() {
        LoginRateLimiter smallLimiter = new LoginRateLimiter(5, 60, 10, mutableClock);
        for (int i = 0; i < 10; i++) {
            assertThat(smallLimiter.tryAcquire("10.0.0." + i)).isTrue();
        }
        assertThat(smallLimiter.getTrackedKeyCount()).isEqualTo(10);

        // 11th distinct key within window -> capacity saturated
        assertThat(smallLimiter.tryAcquire("10.0.0.99")).isFalse();
    }

    @Test
    @DisplayName("GIVEN capacity saturated WHEN window expires THEN new key triggers cleanup and succeeds")
    void testCapacitySaturationRecoversAfterWindowExpiry() {
        LoginRateLimiter smallLimiter = new LoginRateLimiter(5, 60, 10, mutableClock);
        for (int i = 0; i < 10; i++) {
            assertThat(smallLimiter.tryAcquire("10.0.0." + i)).isTrue();
        }
        assertThat(smallLimiter.getTrackedKeyCount()).isEqualTo(10);
        assertThat(smallLimiter.tryAcquire("10.0.0.99")).isFalse();

        // Advance clock past the 60s sliding window
        mutableClock.advanceSeconds(61);

        // Now new key triggers cleanup of expired entries and succeeds
        assertThat(smallLimiter.tryAcquire("10.0.0.99")).isTrue();
        assertThat(smallLimiter.getTrackedKeyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN high-concurrency distinct keys WHEN exceeding capacity THEN map size strictly never exceeds maxTrackedKeys")
    void testConcurrentDistinctKeysNeverExceedCapacityLimit() throws InterruptedException {
        int capacity = 20;
        int totalThreads = 16;
        int totalRequests = 100;

        LoginRateLimiter smallLimiter = new LoginRateLimiter(5, 60, capacity, mutableClock);
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);
        AtomicInteger acceptedCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            final String key = "10.10." + (i / 256) + "." + (i % 256);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (smallLimiter.tryAcquire(key)) {
                        acceptedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Strict hard-cap verification: tracked key count must never exceed configured capacity
        assertThat(smallLimiter.getTrackedKeyCount()).isLessThanOrEqualTo(capacity);
        assertThat(acceptedCount.get()).isLessThanOrEqualTo(capacity);
    }

    @Test
    @DisplayName("R3.6: GIVEN expired timestamps WHEN new request arrives THEN key is removed before adding new timestamp")
    void testKeyEvictionOnWindowExpiration() {
        String ip = "192.168.1.50";

        // Fill up the rate limit
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(ip)).isFalse();
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1);

        // Advance clock past window
        mutableClock.advanceSeconds(61);

        // Now the key should be evicted and new attempt succeeds
        assertThat(rateLimiter.tryAcquire(ip)).isTrue();
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1); // Only current active timestamp
    }

    @Test
    @DisplayName("R3.6: GIVEN same IP active use THEN key is NOT evicted")
    void testActiveIpKeyIsNotEvicted() {
        String ip = "192.168.1.100";

        // Continuous use within window
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire(ip)).isTrue();
            mutableClock.advanceSeconds(10); // Advance 10s, still within 60s window
        }

        // Key should still be tracked
        assertThat(rateLimiter.getTrackedKeyCount()).isEqualTo(1);
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        public MutableClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = initialInstant;
            this.zone = zone;
        }

        public void advanceSeconds(long seconds) {
            this.currentInstant = this.currentInstant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
