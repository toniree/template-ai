package com.templateai.sandbox.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the test clock actually replaces the application's {@code Clock} bean — the part that is
 * easy to get subtly wrong, because a service holding the real clock would still pass every
 * assertion made against the fake one.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MutableClock.Config.class)
class MutableClockIT {

    @Autowired
    private MutableClock testClock;

    /** What a service gets when it asks for a Clock. Must be the same object the test controls. */
    @Autowired
    private Clock injectedClock;

    @Test
    void replacesTheApplicationClockBean() {
        assertThat(injectedClock).isSameAs(testClock);
    }

    @Test
    void advancingMovesTimeForEveryHolderOfTheBean() {
        Instant before = Instant.now(injectedClock);

        testClock.advance(Duration.ofMinutes(5));

        assertThat(Instant.now(injectedClock)).isEqualTo(before.plus(Duration.ofMinutes(5)));
    }

    @Test
    void pinsToAnExactInstantForBoundaryAssertions() {
        Instant midnight = Instant.parse("2030-06-01T00:00:00Z");

        testClock.set(midnight);

        assertThat(Instant.now(injectedClock)).isEqualTo(midnight);
    }
}
