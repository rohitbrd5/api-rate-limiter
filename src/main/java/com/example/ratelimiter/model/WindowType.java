package com.example.ratelimiter.model;

/**
 * Enum representing the available rate limit windows.
 * Supported windows: SECONDS, MINUTES, HOURS
 */
public enum WindowType {
    SECONDS("seconds"),
    MINUTES("minutes"),
    HOURS("hours");

    private final String value;

    WindowType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Get the window duration in seconds based on the WindowType.
     *
     * @return duration in seconds
     */
    public long getDurationSeconds() {
        switch (this) {
            case SECONDS: return 1;
            case MINUTES: return 60;
            case HOURS: return 3600;
            default: return 60;
        }
    }
}