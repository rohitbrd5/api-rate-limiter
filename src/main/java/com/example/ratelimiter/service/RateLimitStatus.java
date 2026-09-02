package com.example.ratelimiter.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status response for a client's rate limit usage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitStatus {
    /** Client identifier */
    private String clientId;

    /** Maximum requests allowed in the window */
    private long limit;

    /** Remaining requests that can be made in the current window */
    private int remaining;

    /** Estimated seconds until the window resets / next token available */
    private long resetInSeconds;

    /**
     * Create a status for an unknown (not-yet-seen) client.
     *
     * @return a RateLimitStatus with zeroed values
     */
    public static RateLimitStatus unknown() {
        return new RateLimitStatus(null, 0, 0, 0);
    }
}