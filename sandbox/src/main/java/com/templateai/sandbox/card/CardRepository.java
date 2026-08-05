package com.templateai.sandbox.card;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * {@code SELECT ... FOR UPDATE} on the card row. The authorization path takes this before
     * summing spend, so two concurrent charges against one card cannot both read the same total
     * and both approve past the limit. Contention is per-card, which is where it belongs.
     *
     * <p>Read paths must keep using {@code findById} — a write lock on a GET would serialize
     * reads for no reason.
     *
     * <p>This must stay a <em>derived</em> query. Pairing {@code @Lock} with an explicit
     * {@code @Query} here emitted plain SQL with no {@code for update} and no error — the lock
     * silently did nothing and the race stayed open. Verified by
     * {@code TransactionApiIT#twoConcurrentChargesCannotBothConsumeTheSameRemainingLimit}, which
     * fails without the lock. If you change this method, check the SQL, not just the green build.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Card> findWithLockById(Long id);
}
