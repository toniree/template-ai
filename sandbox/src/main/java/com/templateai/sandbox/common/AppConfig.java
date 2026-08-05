package com.templateai.sandbox.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Home for app-wide beans. Add here rather than scattering @Configuration classes. */
@Configuration
public class AppConfig {

    /**
     * Inject this instead of calling {@code Instant.now()} directly, so tests can freeze time.
     * Timestamps on financial records are data, not a side effect.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
