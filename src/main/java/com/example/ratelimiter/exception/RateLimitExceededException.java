package com.example.ratelimiter.exception;

/**
 * Exception thrown when a client exceeds their rate limit.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String clientId;

    public RateLimitExceededException(String clientId) {
        super("Rate limit exceeded for client: " + clientId);
        this.clientId = clientId;
    }

    public RateLimitExceededException(String clientId, String message) {
        super(message);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
