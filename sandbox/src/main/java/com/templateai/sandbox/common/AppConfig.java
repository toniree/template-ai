package com.templateai.sandbox.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Home for app-wide beans. Add here rather than scattering @Configuration classes. */
@Configuration
public class AppConfig {

    /**
     * Inject this instead of calling {@code Instant.now()} directly, so a test can freeze time
     * with {@code Clock.fixed(...)} instead of sleeping or asserting on a moving target.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
