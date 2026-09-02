package com.example.ratelimiter.config;

import com.example.ratelimiter.model.ClientLimitConfig;
import com.example.ratelimiter.model.WindowType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RateLimiterConfiguration provides configurable rate limit settings per client.
 * <p>
 * Limits are defined under the {@code rate-limit} prefix in application.yml.
 * Default limits apply for unknown clients.
 * <p>
 * Example configuration:
 * <pre>
 * rate-limit:
 *   default-limit: 60
 *   default-window: seconds
 *   clients:
 *     customerA: { limit: 100, window: minutes }
 *     customerC: { limit: 10, window: seconds }
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimiterConfiguration {

    /** Default limit for unknown clients (not explicitly configured) */
    private int defaultLimit = 60;

    /** Default window type for unknown clients */
    private WindowType defaultWindowType = WindowType.SECONDS;

    /** Map of client-specific rate limits */
    private Map<String, ClientLimitConfig> clients = new HashMap<>();

    /**
     * Get the rate limit configuration for a specific client.
     *
     * @param clientId the client identifier
     * @return the ClientLimitConfig, or null if not configured (will use defaults)
     */
    public ClientLimitConfig getConfigForClient(String clientId) {
        ClientLimitConfig config = clients.get(clientId);
        if (config != null) {
            return config;
        }
        // Return null to signal use of defaults
        return null;
    }

    /**
     * Get the default rate limit configuration for unknown clients.
     *
     * @return default ClientLimitConfig
     */
    public ClientLimitConfig getDefaultConfig() {
        return ClientLimitConfig.builder()
                .limit(defaultLimit)
                .windowType(defaultWindowType)
                .build();
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public WindowType getDefaultWindowType() {
        return defaultWindowType;
    }

    public void setDefaultWindowType(WindowType defaultWindowType) {
        this.defaultWindowType = defaultWindowType;
    }

    public Map<String, ClientLimitConfig> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientLimitConfig> clients) {
        this.clients = clients;
    }
}
