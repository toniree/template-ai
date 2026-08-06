package com.templateai.sandbox.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fires the same operation from N threads at once and reports how many succeeded.
 *
 * <p>Use this when the problem states an invariant that concurrent callers could break — one seat
 * booked once, a balance never overdrawn, a job claimed by one worker. A sequential test proves
 * nothing about that: calling twice in a row passes just as happily against a racy
 * read-then-write, because the second call sees the first one's committed result. Only overlapping
 * attempts exercise the window where the race lives.
 *
 * <pre>{@code
 * var result = Concurrently.run(8, () ->
 *         mockMvc.perform(post("/api/seats/1/book").header(CurrentUser.HEADER, 1))
 *                 .andReturn().getResponse().getStatus() == 200);
 *
 * result.assertExactlyOneWon();
 * }</pre>
 *
 * <p><b>Three things make this test lie</b>, so check them before trusting a green run:
 * <ol>
 *   <li>The test class must not be {@code @Transactional}. Spring rolls that transaction back and
 *       isolates it, so the worker threads cannot see each other's writes and every one of them
 *       "wins".</li>
 *   <li>Each attempt needs its own transaction: go through MockMvc or the service, never a
 *       repository call that would join the test's own transaction.</li>
 *   <li>Assert the losers failed for the right reason (409, not a timeout or a swallowed
 *       exception). {@link Result#failures()} keeps them so you can look.</li>
 * </ol>
 */
public final class Concurrently {

    private Concurrently() {
    }

    /** An attempt that reports whether it won. Checked exceptions are allowed and count as losses. */
    @FunctionalInterface
    public interface Attempt {
        boolean run() throws Exception;
    }

    /**
     * Runs {@code attempt} on {@code threads} threads released simultaneously.
     *
     * <p>Every thread is started and parked on a latch first, so they contend for real rather than
     * trickling in as the pool spins each one up — without the latch the first thread often
     * finishes before the last one starts, and the test silently stops testing anything.
     */
    public static Result run(int threads, Attempt attempt) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger wins = new AtomicInteger();
        List<Throwable> failures = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (attempt.run()) {
                            wins.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            await(ready, "worker threads to start");
            start.countDown();
            await(done, "concurrent attempts to finish");
        } finally {
            pool.shutdownNow();
        }
        return new Result(threads, wins.get(), List.copyOf(failures));
    }

    private static void await(CountDownLatch latch, String what) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for " + what);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for " + what, e);
        }
    }

    /**
     * @param attempts how many threads ran
     * @param wins     how many reported success
     * @param failures exceptions thrown by attempts — usually empty, since a rejected attempt
     *                 normally returns {@code false} rather than throwing
     */
    public record Result(int attempts, int wins, List<Throwable> failures) {

        /** The usual assertion: the invariant held, exactly one caller got the resource. */
        public void assertExactlyOneWon() {
            assertWinnersWere(1);
        }

        /** For capacity limits — "only N of the M who tried could fit". */
        public void assertWinnersWere(int expected) {
            assertThat(failures)
                    .as("attempts threw instead of returning a win/lose result")
                    .isEmpty();
            assertThat(wins)
                    .as("%d concurrent attempts, expected %d to succeed", attempts, expected)
                    .isEqualTo(expected);
        }
    }
}
