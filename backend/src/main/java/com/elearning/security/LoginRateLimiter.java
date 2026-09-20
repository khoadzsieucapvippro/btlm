package com.elearning.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, in-memory sliding window rate limiter designed for brute-force protection
 * on authentication endpoints (e.g. POST /api/v1/auth/login).
 * <p>
 * Operational boundaries:
 * - Local to a single application instance (single-node deployment).
 * - Multi-instance cluster deployments should configure rate limiting at the reverse proxy/gateway layer.
 * - Active memory management: dead keys with expired timestamps are systematically pruned to prevent
 *   unbounded memory growth (OWASP API4:2023 Unrestricted Resource Consumption).
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMillis;
    private final int maxTrackedKeys;
    private final Clock clock;
    private final Map<String, Deque<Long>> attemptHistory = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMillis = new AtomicLong(0);

    @org.springframework.beans.factory.annotation.Autowired
    public LoginRateLimiter(
            @Value("${app.rate-limit.login.max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.login.window-seconds:60}") int windowSeconds) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    public LoginRateLimiter(int maxAttempts, int windowSeconds, Clock clock) {
        this(maxAttempts, windowSeconds, 10000, clock);
    }

    public LoginRateLimiter(int maxAttempts, int windowSeconds, int maxTrackedKeys, Clock clock) {
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 10;
        this.windowMillis = (windowSeconds > 0 ? windowSeconds : 60) * 1000L;
        this.maxTrackedKeys = maxTrackedKeys > 0 ? maxTrackedKeys : 10000;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * Checks if a request from the given client key (e.g. client IP) is permitted under the rate limit.
     * Records the attempt timestamp if allowed.
     *
     * @param key client identifier (e.g. remote IP or composite key)
     * @return true if permitted, false if rate limit exceeded or capacity saturated
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }

        long now = clock.millis();
        long windowStart = now - windowMillis;

        // Opportunistic active eviction of expired keys (throttled to prevent CPU burn)
        long last = lastCleanupMillis.get();
        if ((now - last > windowMillis || (attemptHistory.size() > 500 && now - last > 5000L))
                && lastCleanupMillis.compareAndSet(last, now)) {
            cleanupExpiredEntries(now);
        }

        // Guard against memory exhaustion under active high-volume key generation DDoS.
        // Saturated cleanup is throttled (minimum 1000ms cooldown) so high-concurrency bursts
        // fail-closed in O(1) time without triggering massive map iteration storms.
        if (!attemptHistory.containsKey(key) && attemptHistory.size() >= maxTrackedKeys) {
            long lastCap = lastCleanupMillis.get();
            if (now - lastCap >= 1000L && lastCleanupMillis.compareAndSet(lastCap, now)) {
                cleanupExpiredEntries(now);
            }
            if (attemptHistory.size() >= maxTrackedKeys) {
                return false; // Fail-closed on capacity saturation
            }
        }

        while (true) {
            Deque<Long> timestamps = attemptHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

            synchronized (timestamps) {
                // Ensure the deque wasn't removed concurrently between computeIfAbsent and synchronized
                if (attemptHistory.get(key) != timestamps) {
                    continue;
                }

                // Evict timestamps outside sliding window
                while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                    timestamps.pollFirst();
                }

                // Strict hard-cap enforcement: if concurrent insertions pushed size over maxTrackedKeys,
                // evict the newly created empty deque and reject
                if (timestamps.isEmpty() && attemptHistory.size() > maxTrackedKeys) {
                    attemptHistory.remove(key, timestamps);
                    return false;
                }

                if (timestamps.size() < maxAttempts) {
                    timestamps.addLast(now);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Proactively cleans up all keys whose sliding window history has expired.
     */
    public void cleanupExpiredEntries() {
        cleanupExpiredEntries(clock.millis());
    }

    /**
     * Cleans up keys whose timestamps have expired relative to the given reference epoch millis.
     * Uses atomic removal on ConcurrentHashMap under deque synchronization to prevent race conditions.
     *
     * @param now reference epoch millis
     */
    public void cleanupExpiredEntries(long now) {
        long windowStart = now - windowMillis;
        for (Map.Entry<String, Deque<Long>> entry : attemptHistory.entrySet()) {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                    timestamps.pollFirst();
                }
                if (timestamps.isEmpty()) {
                    attemptHistory.remove(entry.getKey(), timestamps);
                }
            }
        }
    }

    /**
     * Clears attempt history for all keys or a specific key (useful for test resets).
     */
    public void reset() {
        attemptHistory.clear();
    }

    public void reset(String key) {
        if (key != null) {
            attemptHistory.remove(key);
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getWindowMillis() {
        return windowMillis;
    }

    /**
     * Returns the current number of IP keys retained in the rate limiter.
     * Public for testing purposes.
     *
     * @return number of distinct IP keys currently tracked
     */
    public int getTrackedKeyCount() {
        return attemptHistory.size();
    }
}
