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
 *   cleanup:
 *     idle-timeout: 300000
 *     sweep-interval: 60000
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

    /** Cleanup configuration (idle timeout and sweep interval) */
    private Cleanup cleanup = new Cleanup();

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

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    /**
     * Get the idle timeout in milliseconds.
     *
     * @return idle timeout in ms
     */
    public long getIdleTimeout() {
        return cleanup != null ? cleanup.getIdleTimeout() : 300000L;
    }

    /**
     * Get the sweep interval in milliseconds.
     *
     * @return sweep interval in ms
     */
    public long getSweepInterval() {
        return cleanup != null ? cleanup.getSweepInterval() : 60000L;
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

    /**
     * Cleanup configuration for inactive client removal.
     */
    public static class Cleanup {
        /** Idle timeout in milliseconds after which an inactive client is removed */
        private long idleTimeout = 300000L; // 5 minutes default

        /** Sweep interval in milliseconds - how often the cleanup task runs */
        private long sweepInterval = 60000L; // 1 minute default

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getSweepInterval() {
            return sweepInterval;
        }

        public void setSweepInterval(long sweepInterval) {
            this.sweepInterval = sweepInterval;
        }
    }
}
