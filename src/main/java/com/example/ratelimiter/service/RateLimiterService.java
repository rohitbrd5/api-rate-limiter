package com.example.ratelimiter.service;

import com.example.ratelimiter.config.RateLimiterConfiguration;
import com.example.ratelimiter.model.ClientLimitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service-level rate limiter that manages per-client rate limiters.
 * <p>
 * Key design decisions:
 * - Uses ConcurrentHashMap for thread-safe client management
 * - Creates ClientRateLimiter lazily on first request per client
 * - Memory bounded: no accumulation of timestamps or history
 * - Configurable limits via RateLimiterConfiguration
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    /** Map of per-client rate limiters (ConcurrentHashMap for thread safety) */
    private final RateLimiterConfiguration rateLimiterConfig;
    private final ConcurrentHashMap<String, ClientRateLimiter> clientLimiters = new ConcurrentHashMap<>();

    /**
     * Check if a request is allowed for the given client.
     * <p>
     * Behavior:
     * - If client has a configured limit, use that
     * - If client is unknown, use the default limit
     * - Thread-safe: safe for concurrent calls
     *
     * @param clientId the client identifier (e.g., "customerA", "customerB")
     * @return true if the request is allowed, false if rate limited
     */
    public boolean allowRequest(String clientId) {
        // Get the per-client limiter, creating it if needed
        ClientRateLimiter clientLimiter = clientLimiters.computeIfAbsent(clientId, this::createClientLimiter);

        // Try to acquire a token
        return clientLimiter.tryAcquire();
    }

    /**
     * Create a new ClientRateLimiter for the given client ID.
     * Uses the configured limit if available, otherwise defaults.
     *
     * @param clientId the client identifier
     * @return a new ClientRateLimiter instance
     */
    private ClientRateLimiter createClientLimiter(String clientId) {
        ClientLimitConfig config = rateLimiterConfig.getConfigForClient(clientId);
        if (config == null) {
            config = rateLimiterConfig.getDefaultConfig();
        }
        return new ClientRateLimiter(config);
    }

    /**
     * Get the current usage status for a client.
     *
     * @param clientId the client identifier
     * @return status including remaining requests and reset time
     */
    public RateLimitStatus getStatus(String clientId) {
        ClientRateLimiter limiter = clientLimiters.get(clientId);
        if (limiter == null) {
            return RateLimitStatus.unknown();
        }

        long available = limiter.getAvailableTokens();
        ClientLimitConfig config = limiter.getConfig();
        long total = config.getLimit();
        long remaining = available;

        long windowSeconds = config.getWindowType().getDurationSeconds();
        long estimatedResetSeconds = 0;

        if (available >= total) {
            // All tokens consumed - reset in a full window
            estimatedResetSeconds = windowSeconds;
        } else {
            // Tokens partially consumed - estimate based on elapsed time
            // Simplified: return how long until a full refill
            estimatedResetSeconds = windowSeconds;
        }

        return new RateLimitStatus(
                clientId,
                total,
                (int) remaining,
                estimatedResetSeconds
        );
    }
}