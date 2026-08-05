package com.templateai.sandbox.transaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByCardIdOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Spend on one card. Aggregated in the database — never load rows to sum them in Java. */
    @Query("""
            select coalesce(sum(t.amountMinor), 0)
            from Transaction t
            where t.card.id = :cardId and t.status = :status
            """)
    long sumAmountByCardAndStatus(@Param("cardId") Long cardId, @Param("status") Transaction.Status status);

    /**
     * Spend for every card in one query. The card list endpoint uses this instead of calling
     * {@link #sumAmountByCardAndStatus} per row, which would be a textbook N+1.
     */
    @Query("""
            select t.card.id as cardId, coalesce(sum(t.amountMinor), 0) as spentMinor
            from Transaction t
            where t.status = :status
            group by t.card.id
            """)
    List<CardSpend> sumAmountGroupedByCard(@Param("status") Transaction.Status status);

    /** Projection interface — Spring Data implements it, no class needed. */
    interface CardSpend {
        Long getCardId();

        long getSpentMinor();
    }
}
