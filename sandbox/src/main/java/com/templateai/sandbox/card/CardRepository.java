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
     * <p>Keep this a <em>derived</em> finder. On this stack (Boot 3.4 / Spring Data JPA 3.4 /
     * Hibernate 6.6 / H2), the identical {@code @Lock} placed on an explicit
     * {@code @Query("select c from Card c where c.id = :id")} generated SQL with no
     * {@code for update} and raised no warning, so the lock silently did nothing. That is an
     * observation about this configuration, not a general rule about {@code @Lock} — but it does
     * mean a green build proves nothing here. If you change how this row is loaded, set
     * {@code spring.jpa.show-sql=true} and confirm {@code for update} appears in the generated SQL.
     * {@code TransactionApiIT#twoConcurrentChargesCannotBothConsumeTheSameRemainingLimit} fails
     * when the lock is absent, so run it too.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Card> findWithLockById(Long id);
}
