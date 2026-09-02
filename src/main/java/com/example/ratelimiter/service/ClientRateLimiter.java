package com.example.ratelimiter.service;

import com.example.ratelimiter.model.ClientLimitConfig;
import com.example.ratelimiter.model.WindowType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-client rate limiter using the token bucket algorithm.
 * <p>
 * This implementation provides:
 * - Thread-safe operations using atomic primitives
 * - Memory-bounded behavior: no timestamp queue, only stores count + last refill time
 * - Lazy token refill on each request
 * <p>
 * The token bucket is ideal for memory-bounded rate limiting because it doesn't
 * require storing individual request timestamps.
 */
public class ClientRateLimiter {

    /** Current configuration (can be updated atomically) */
    private final AtomicReference<ClientLimitConfig> config;

    /** Available tokens (capacity never exceeds the limit) */
    private final java.util.concurrent.atomic.AtomicLong availableTokens;

    /** Timestamp (in nanoseconds) when tokens were last refilled */
    private volatile long lastRefillNanos;

    /**
     * Create a new client rate limiter with the given configuration.
     *
     * @param initialConfig initial rate limit configuration
     */
    public ClientRateLimiter(ClientLimitConfig initialConfig) {
        this.config = new AtomicReference<>(initialConfig);
        this.availableTokens = new java.util.concurrent.atomic.AtomicLong(initialConfig.getLimit());
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Try to consume one token (i.e., allow one request).
     * <p>
     * Algorithm:
     * 1. Calculate tokens to add based on time elapsed since last refill
     * 2. Cap tokens at the configured limit
     * 3. If available tokens >= 1, decrement and return true
     * 4. Otherwise, return false
     *
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryAcquire() {
        ClientLimitConfig currentConfig = config.get();
        long now = System.nanoTime();

        // Refill tokens based on time elapsed
        refillTokens(currentConfig, now);

        // Try to consume one token
        long current;
        do {
            current = availableTokens.get();
            if (current <= 0) {
                return false;
            }
        } while (!availableTokens.compareAndSet(current, current - 1));

        return true;
    }

    /**
     * Refill tokens based on elapsed time. Uses a synchronized block since
     * updating the lastRefillNanos is not an atomic operation.
     *
     * @param currentConfig the current rate limit configuration
     * @param now current time in nanoseconds
     */
    private void refillTokens(ClientLimitConfig currentConfig, long now) {
        long windowNanos = currentConfig.getWindowType().getDurationSeconds() * 1_000_000_000L;
        int limit = currentConfig.getLimit();

        synchronized (this) {
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            // Calculate tokens to add: (elapsed / window) * limit
            long tokensToAdd = (elapsed * limit) / windowNanos;
            if (tokensToAdd > 0) {
                long updated = Math.min(limit, availableTokens.get() + tokensToAdd);
                availableTokens.set(updated);
                lastRefillNanos = now;
            }
        }
    }

    /**
     * Update the rate limit configuration for this client.
     *
     * @param newConfig the new configuration
     */
    public void updateConfig(ClientLimitConfig newConfig) {
        this.config.set(newConfig);
        // Reset available tokens to new limit (don't penalize or reward beyond new limit)
        this.availableTokens.set(Math.min(availableTokens.get(), newConfig.getLimit()));
    }

    /**
     * Get the current configuration.
     *
     * @return the current ClientLimitConfig
     */
    public ClientLimitConfig getConfig() {
        return config.get();
    }

    /**
     * Get the current available token count.
     *
     * @return the number of available tokens
     */
    public long getAvailableTokens() {
        return availableTokens.get();
    }

    /**
     * Get the number of milliseconds until the next token is available.
     * Returns 0 if tokens are available now.
     *
     * @return milliseconds until next available token, or 0 if available now
     */
    public long getMillisUntilNextToken() {
        ClientLimitConfig currentConfig = config.get();
        if (availableTokens.get() > 0) {
            return 0;
        }
        long windowNanos = currentConfig.getWindowType().getDurationSeconds() * 1_000_000_000L;
        long nanosPerToken = windowNanos / currentConfig.getLimit();
        long elapsed = System.nanoTime() - lastRefillNanos;
        if (elapsed >= nanosPerToken) {
            return 0;
        }
        return (nanosPerToken - elapsed) / 1_000_000L;
    }

    /**
     * Get the last refill time in nanoseconds.
     * Used by the service for status reporting.
     *
     * @return last refill time in nanoseconds
     */
    public long getConfigAsNanos() {
        return lastRefillNanos;
    }
}