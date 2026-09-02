package com.example.ratelimiter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiter.exception.RateLimitExceededException;
import com.example.ratelimiter.service.RateLimitStatus;
import com.example.ratelimiter.service.RateLimiterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ratelimit")
@Tag(name = "Rate Limiter", description = "API rate limiting service endpoints")
@RequiredArgsConstructor
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    /**
     * Check if a request is allowed for the given client.
     * <p>
     * If the rate limit is exceeded, returns false.
     *
     * @param clientId the client identifier
     * @return ResponseEntity with {allowed: boolean} and optional usage info
     */
    @Operation(summary = "Check if request is allowed", description = "Check if a request from the given client is allowed under the rate limit")
    @ApiResponse(responseCode = "200", description = "Request allowed or rejected",
            content = @Content(schema = @Schema(implementation = AllowRequestResponse.class)))
    @GetMapping("/{clientId}/check")
    public ResponseEntity<AllowRequestResponse> checkRequest(@PathVariable String clientId) {
        boolean allowed = rateLimiterService.allowRequest(clientId);
        if (!allowed) {
            throw new RateLimitExceededException(clientId);
        }
        AllowRequestResponse response = new AllowRequestResponse(allowed, clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current rate limit status for a client.
     *
     * @param clientId the client identifier
     * @return ResponseEntity with rate limit status
     */
    @Operation(summary = "Get rate limit status", description = "Get current usage statistics for a client")
    @ApiResponse(responseCode = "200", description = "Rate limit status",
            content = @Content(schema = @Schema(implementation = RateLimitStatus.class)))
    @GetMapping("/{clientId}/status")
    public ResponseEntity<RateLimitStatus> getStatus(@PathVariable String clientId) {
        RateLimitStatus status = rateLimiterService.getStatus(clientId);
        return ResponseEntity.ok(status);
    }

    /**
     * Response body for allow request check.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllowRequestResponse {
        /** Whether the request is allowed */
        private boolean allowed;

        /** Client identifier */
        private String clientId;

    }
}