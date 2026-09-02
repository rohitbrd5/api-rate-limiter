package com.example.ratelimiter.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
    @DisplayName("should return 429 after exhausting customerD limit of 5")
    void checkEndpoint_returns429WhenLimitExhausted() throws Exception {
        String clientId = "customerD";

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
        String clientA = "customerD";
        String clientB = "clientB-" + System.currentTimeMillis();

        // Exhaust clientA
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/ratelimit/{clientA}/check", clientA))
                    .andExpect(status().isOk());
        }

        // clientA should be rate limited
        mockMvc.perform(get("/api/ratelimit/{clientA}/check", clientA))
                .andExpect(status().isTooManyRequests());

        // clientB should still work
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
}
