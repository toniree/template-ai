package com.templateai.sandbox.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.common.CurrentUser;

/**
 * Proves the two support helpers do what their javadoc claims, so a test written against them under
 * time pressure can be trusted. No Spring context — these are plain and fast.
 */
class SupportTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ---- Concurrently ------------------------------------------------------

    /**
     * The harness's core promise. A compare-and-set stands in for whatever the real invariant is —
     * a conditional UPDATE, a unique constraint — so this stays deterministic while still proving
     * the counting is right.
     */
    @Test
    void reportsExactlyOneWinnerWhenOnlyOneAttemptCanSucceed() {
        AtomicBoolean taken = new AtomicBoolean(false);

        Concurrently.run(8, () -> taken.compareAndSet(false, true)).assertExactlyOneWon();
    }

    /** Capacity limits: N winners out of M attempts. */
    @Test
    void reportsMultipleWinnersWhenTheLimitAllowsThem() {
        AtomicInteger seatsLeft = new AtomicInteger(3);

        Concurrently.run(10, () -> seatsLeft.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0)
                .assertWinnersWere(3);
    }

    /**
     * The threads must genuinely overlap — if they ran one after another the harness would prove
     * nothing, and every racy implementation would pass.
     */
    @Test
    void releasesAllThreadsAtOnceRatherThanLettingThemTrickleThrough() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        Concurrently.run(6, () -> {
            int now = inFlight.incrementAndGet();
            peak.accumulateAndGet(now, Math::max);
            Thread.sleep(50);            // hold the overlap open long enough to observe
            inFlight.decrementAndGet();
            return true;
        });

        assertThat(peak.get())
                .as("attempts ran sequentially, so the harness would not detect a race")
                .isGreaterThan(1);
    }

    /** A thrown attempt must surface, not be silently counted as a loss. */
    @Test
    void surfacesExceptionsInsteadOfSwallowingThem() {
        Concurrently.Result result = Concurrently.run(4, () -> {
            throw new IllegalStateException("boom");
        });

        assertThat(result.wins()).isZero();
        assertThat(result.failures()).hasSize(4);
        assertThatThrownBy(result::assertExactlyOneWon)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("threw instead of returning");
    }

    // ---- CurrentUser -------------------------------------------------------

    @Test
    void readsTheCallerIdFromTheHeader() {
        givenRequestWithHeader("42");

        assertThat(currentUser.require()).isEqualTo(42L);
        assertThat(currentUser.find()).isEqualTo(42L);
    }

    @Test
    void requireIsUnauthorizedWhenTheHeaderIsMissing() {
        givenRequestWithHeader(null);

        assertThatThrownBy(currentUser::require)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(CurrentUser.HEADER);
        assertThat(currentUser.find()).isNull();
    }

    /** A garbage header is "no identity" (401), never a 500 from a parse failure. */
    @Test
    void requireIsUnauthorizedWhenTheHeaderIsNotANumber() {
        givenRequestWithHeader("not-a-number");

        assertThatThrownBy(currentUser::require).isInstanceOf(ApiException.class);
        assertThat(currentUser.find()).isNull();
    }

    /** Outside a request — a scheduled task, a worker thread — there is simply no caller. */
    @Test
    void findIsNullOutsideOfARequest() {
        assertThat(currentUser.find()).isNull();
    }

    private void givenRequestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(CurrentUser.HEADER, value);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
