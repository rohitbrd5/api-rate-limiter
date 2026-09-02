package com.example.ratelimiter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that periodically cleans up inactive client rate limiters.
 * <p>
 * Runs at a configurable interval ({@code rate-limit.cleanup.sweep-interval})
 * and removes clients that haven't been accessed for longer than the
 * configured idle timeout ({@code rate-limit.cleanup.idle-timeout}).
 */
@Slf4j
@Component
public class InactiveClientCleanupTask {

    private final RateLimiterService rateLimiterService;

    public InactiveClientCleanupTask(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Runs cleanup at the configured sweep interval.
     * <p>
     * The interval is configured via {@code rate-limit.cleanup.sweep-interval}
     * in milliseconds (default: 60000 ms = 1 minute).
     */
    @Scheduled(fixedRateString = "${rate-limit.cleanup.sweep-interval:60000}")
    public void cleanupInactiveClients() {
        long now = System.nanoTime();
        int beforeCount = rateLimiterService.getActiveClientCount();

        rateLimiterService.cleanupInactiveLimiters(now);

        int afterCount = rateLimiterService.getActiveClientCount();
        int removed = beforeCount - afterCount;

        log.info("Inactive client cleanup completed. Removed {} inactive client(s)", removed);
    }
}
