package com.templateai.sandbox.card;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.card.CardDtos.CardResponse;
import com.templateai.sandbox.card.CardDtos.CreateCardRequest;
import com.templateai.sandbox.card.CardDtos.UpdateCardRequest;
import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.transaction.TransactionRepository;
import com.templateai.sandbox.transaction.TransactionRepository.CardSpend;
import com.templateai.sandbox.transaction.Transaction;

/**
 * All card business rules live here. The controller does HTTP, the repository does SQL, and
 * entities never escape this layer — they are mapped to DTOs before the transaction closes.
 */
@Service
@Transactional
public class CardService {

    private final CardRepository cards;
    private final TransactionRepository transactions;
    private final Clock clock;

    public CardService(CardRepository cards, TransactionRepository transactions, Clock clock) {
        this.cards = cards;
        this.transactions = transactions;
        this.clock = clock;
    }

    public CardResponse create(CreateCardRequest request) {
        Card card = new Card();
        card.setCardholderName(request.cardholderName());
        card.setLast4(request.last4());
        card.setSpendLimitMinor(request.spendLimitMinor());
        card.setCurrency(request.currency());
        card.setStatus(Card.Status.ACTIVE);
        card.setCreatedAt(Instant.now(clock));
        return CardResponse.from(cards.save(card), 0L);
    }

    /** Two queries total regardless of card count: one for the cards, one for all spend. */
    @Transactional(readOnly = true)
    public List<CardResponse> list() {
        Map<Long, Long> spentByCard = transactions.sumAmountGroupedByCard(Transaction.Status.APPROVED).stream()
                .collect(Collectors.toMap(CardSpend::getCardId, CardSpend::getSpentMinor));

        return cards.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(card -> CardResponse.from(card, spentByCard.getOrDefault(card.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse get(Long id) {
        Card card = require(id);
        return CardResponse.from(card, spent(id));
    }

    /**
     * Cards are never deleted — a card is the parent of immutable financial records, so the
     * lifecycle is ACTIVE -> FROZEN -> CANCELLED. Cancelling is terminal.
     */
    public CardResponse update(Long id, UpdateCardRequest request) {
        Card card = require(id);

        // Terminal means terminal: not the status, not the limit, nothing.
        if (card.getStatus() == Card.Status.CANCELLED) {
            throw ApiException.conflict("Card " + id + " is cancelled; cancellation is terminal");
        }
        if (request.status() != null && request.status() != card.getStatus()) {
            requireLegalTransition(card.getStatus(), request.status());
            card.setStatus(request.status());
        }
        if (request.spendLimitMinor() != null) {
            card.setSpendLimitMinor(request.spendLimitMinor());
        }
        return CardResponse.from(card, spent(id));
    }

    /**
     * The whole lifecycle, in one place: ACTIVE and FROZEN convert into each other, either can be
     * CANCELLED, and CANCELLED is the end. Written as an exhaustive switch so adding a status is a
     * compile error here rather than a silently-permitted transition.
     */
    private void requireLegalTransition(Card.Status from, Card.Status to) {
        boolean legal = switch (from) {
            case ACTIVE -> to == Card.Status.FROZEN || to == Card.Status.CANCELLED;
            case FROZEN -> to == Card.Status.ACTIVE || to == Card.Status.CANCELLED;
            case CANCELLED -> false;
        };
        if (!legal) {
            throw ApiException.conflict("Card cannot move from %s to %s".formatted(from, to));
        }
    }

    /** Shared 404 so every "unknown card" path returns the identical error body. */
    @Transactional(readOnly = true)
    public Card require(Long id) {
        return cards.findById(id).orElseThrow(() -> ApiException.notFound("Card " + id + " not found"));
    }

    /**
     * Same lookup, but locks the row for the rest of the caller's transaction. Use this whenever
     * you are about to make a decision from the card's spend and then write based on it — the
     * check and the write have to be one atomic step or the limit is only advisory.
     */
    public Card requireForUpdate(Long id) {
        return cards.findWithLockById(id).orElseThrow(() -> ApiException.notFound("Card " + id + " not found"));
    }

    private long spent(Long cardId) {
        return transactions.sumAmountByCardAndStatus(cardId, Transaction.Status.APPROVED);
    }
}
