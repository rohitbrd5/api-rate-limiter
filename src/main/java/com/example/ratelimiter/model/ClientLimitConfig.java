package com.example.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class representing a client's rate limit.
 * <p>
 * Each client is configured with a maximum number of requests (limit)
 * over a specific time window (windowType).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientLimitConfig {
    /** Maximum number of allowed requests in the window */
    private int limit;

    /** Time window for the rate limit (e.g., MINUTES, SECONDS) */
    private WindowType windowType;
}