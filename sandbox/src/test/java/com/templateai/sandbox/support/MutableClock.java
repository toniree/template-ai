package com.templateai.sandbox.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A {@link Clock} the test moves by hand, so expiry and TTL rules can be proven without sleeping.
 *
 * <p>Any behaviour that depends on time — a hold that lapses after five minutes, a token that
 * expires, a retry backoff, a "closes at" cutoff — is otherwise testable only by waiting, which
 * makes the suite slow and flaky. Advance this instead: the assertion becomes exact, and a
 * five-minute rule costs no wall-clock time to verify.
 *
 * <p>Import the nested configuration on the test class that needs it:
 *
 * <pre>{@code
 * @Import(MutableClock.Config.class)
 * class HoldExpiryIT extends ApiIntegrationTest {
 *
 *     @Autowired MutableClock clock;
 *
 *     @Test
 *     void holdLapsesAfterFiveMinutes() throws Exception {
 *         // ... place a hold ...
 *         clock.advance(Duration.ofMinutes(5).plusSeconds(1));
 *         // ... assert the seat is bookable again ...
 *     }
 * }
 * }</pre>
 *
 * <p>This only works for code that injects the {@code Clock} bean and calls
 * {@code Instant.now(clock)}. A direct {@code Instant.now()} ignores it entirely — which is the
 * practical reason the scaffold insists on the injected clock.
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant now;

    public MutableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    /** Moves time forward. Negative durations work too, for "what if this is already stale". */
    public void advance(Duration amount) {
        now = now.plus(amount);
    }

    /** Pins the clock to an exact instant — for asserting on a specific date or boundary. */
    public void set(Instant instant) {
        now = instant;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        return new MutableClock(now, otherZone);
    }

    /**
     * Replaces the application's {@code Clock} bean for tests that {@code @Import} it.
     * {@code @Primary} so existing injection points asking for {@link Clock} get this one without
     * any production code knowing the difference.
     *
     * <p>The method is deliberately <b>not</b> called {@code clock()}: that is the bean name
     * {@code AppConfig} already uses, and Spring Boot disables bean-definition overriding, so a
     * matching name fails the whole context with "a bean with that name has already been defined".
     * A different name plus {@code @Primary} leaves both beans registered and lets type-based
     * injection pick this one.
     */
    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        public MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));
        }
    }
}
