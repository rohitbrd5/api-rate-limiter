package com.example.ratelimiter.integration;

import com.example.ratelimiter.config.RateLimiterConfiguration;
import com.example.ratelimiter.model.ClientLimitConfig;
import com.example.ratelimiter.model.WindowType;
import com.example.ratelimiter.service.ClientRateLimiter;
import com.example.ratelimiter.service.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-context integration tests for the rate limiter API.
 * <p>
 * Boots the entire Spring application and exercises the controller endpoints
 * via MockMvc, verifying the full stack (controller → service → client limiter).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiterConfiguration rateLimiterConfig;

  
    @Test
    @DisplayName("should return 200 OK for first request")
    void checkEndpoint_returns200ForFirstRequest() throws Exception {
        String clientId = "client-" + System.currentTimeMillis();

        mockMvc.perform(get("/api/ratelimit/{clientId}/check", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    @DisplayName("should return 429 after exhausting rate limit")
    void checkEndpoint_returns429WhenLimitExhausted() throws Exception {
        // exhaustTestClient has limit=5/second, so sequential requests can exhaust it.
        String clientId = "exhaustTestClient";

        // Exhaust limit (5 requests)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/ratelimit/{clientId}/check", clientId))
                    .andExpect(status().isOk());
        }

        // Next request should be rate limited
        mockMvc.perform(get("/api/ratelimit/{clientId}/check", clientId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    @DisplayName("should isolate rate limits between clients")
    void checkEndpoint_isolatesBetweenClients() throws Exception {
        // Use burstClient (limit=5/second) for clientA to make exhaustion test fast.
        // burstClient is already configured with limit=5 in test application.yml.
        // clientB uses the default limit (60/sec) and is a separate limiter.
        String clientA = "burstClient";
        String clientB = "isolatedClientB-" + System.nanoTime();

        // Exhaust clientA (5 requests)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/ratelimit/{clientA}/check", clientA))
                    .andExpect(status().isOk());
        }

        // clientA should be rate limited
        mockMvc.perform(get("/api/ratelimit/{clientA}/check", clientA))
                .andExpect(status().isTooManyRequests());

        // clientB should still work (separate limiter with default limit)
        mockMvc.perform(get("/api/ratelimit/{clientB}/check", clientB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("should allow 100 requests for customerA")
    void checkEndpoint_allows100RequestsForCustomerA() throws Exception {
        // customerA is configured for 100 requests/minute
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/ratelimit/customerA/check"))
                    .andExpect(status().isOk());
        }

        // 101st should be rate limited
        mockMvc.perform(get("/api/ratelimit/customerA/check"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("should allow 10 requests for customerC")
    void checkEndpoint_allows10RequestsForCustomerC() throws Exception {
        // customerC is configured for 10 requests/second
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/ratelimit/customerC/check"))
                    .andExpect(status().isOk());
        }

        // 11th should be rate limited
        mockMvc.perform(get("/api/ratelimit/customerC/check"))
                .andExpect(status().isTooManyRequests());
    }

   
    @Test
    @DisplayName("should return correct status after requests")
    void statusEndpoint_returnsCorrectStatus() throws Exception {
        String clientId = "status-client-" + System.currentTimeMillis();

        // Make 3 requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/ratelimit/{clientId}/check", clientId))
                    .andExpect(status().isOk());
        }

        // Check status
        mockMvc.perform(get("/api/ratelimit/{clientId}/status", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.limit").value(60))
                .andExpect(jsonPath("$.remaining").value(57));
    }

    @Test
    @DisplayName("should return zero values for never-seen client")
    void statusEndpoint_returnsZeroForUnknownClient() throws Exception {
        String clientId = "unknown-" + System.currentTimeMillis();

        mockMvc.perform(get("/api/ratelimit/{clientId}/status", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(0))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.resetInSeconds").value(0));
    }

   
    @Test
    @DisplayName("should return UP for health check")
    void healthEndpoint_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("should clean up inactive clients based on idle timeout")
    void cleanupInactiveClients_removesClientsExceedingIdleTimeout() {
        // First, get the RateLimiterService from the application context
        RateLimiterService rateLimiterService = getRateLimiterService();

        // Add an old client whose last access is well past the idle timeout
        String oldClientId = "old-client-" + System.currentTimeMillis();
        rateLimiterService.allowRequest(oldClientId);
        ClientRateLimiter oldLimiter = rateLimiterService.getClientLimiter(oldClientId);
        // Set last access to 200ms ago (well beyond the 50ms test timeout)
        long now = System.nanoTime();
        oldLimiter.setLastAccessNanos(now - TimeUnit.MILLISECONDS.toNanos(200));

        // Add a recent client
        String recentClientId = "recent-client-" + System.currentTimeMillis();
        rateLimiterService.allowRequest(recentClientId);
        ClientRateLimiter recentLimiter = rateLimiterService.getClientLimiter(recentClientId);
        recentLimiter.setLastAccessNanos(now);

        // Sanity: both clients are in the map before cleanup
        assertTrue(rateLimiterService.getClientLimiters().containsKey(oldClientId),
                "Old client should be present before cleanup");
        assertTrue(rateLimiterService.getClientLimiters().containsKey(recentClientId),
                "Recent client should be present before cleanup");

        // Run cleanup with current time
        rateLimiterService.cleanupInactiveLimiters(now);

        // The old client should have been removed
        assertFalse(rateLimiterService.getClientLimiters().containsKey(oldClientId),
                "Old client should have been removed from the map");

        // The recent client should still be present
        assertTrue(rateLimiterService.getClientLimiters().containsKey(recentClientId),
                "Recent client should still be present after cleanup");
    }

    /**
     * Helper to get the RateLimiterService from the application context.
     */
    private RateLimiterService getRateLimiterService() {
        return ApplicationContextProvider.getBean(RateLimiterService.class);
    }
}