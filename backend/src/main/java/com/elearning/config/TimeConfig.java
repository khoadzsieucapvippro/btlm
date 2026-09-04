package com.elearning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuration for application business timezone and time sources (BE-SRS-001).
 * Provides a Spring-managed {@link Clock} configured with the explicit business timezone
 * to eliminate dependency on host/JVM system default timezone.
 */
@Configuration
public class TimeConfig {

    public static final String DEFAULT_BUSINESS_TIMEZONE = "Asia/Ho_Chi_Minh";

    @Bean
    public Clock businessClock(@Value("${app.business-timezone:" + DEFAULT_BUSINESS_TIMEZONE + "}") String businessTimezone) {
        if (businessTimezone == null || businessTimezone.trim().isEmpty()) {
            throw new IllegalArgumentException("app.business-timezone must not be empty");
        }
        // Explicitly validates the IANA ZoneId and fails fast on startup if invalid
        ZoneId zoneId = ZoneId.of(businessTimezone.trim());
        return Clock.system(zoneId);
    }
}
