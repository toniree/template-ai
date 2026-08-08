package com.templateai.sandbox.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Home for app-wide beans. Add here rather than scattering @Configuration classes. */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    /**
     * Serve the walkthrough at {@code /help}. Static resources are only matched by their exact
     * filename, so without this {@code /help} is a 404 and only {@code /help.html} works.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/help").setViewName("forward:/help.html");
    }

    /**
     * Inject this instead of calling {@code Instant.now()} directly, so a test can freeze time
     * with {@code Clock.fixed(...)} instead of sleeping or asserting on a moving target.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
