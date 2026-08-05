package com.templateai.sandbox.transaction;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.card.Card;
import com.templateai.sandbox.card.CardService;
import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.common.PageResponse;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeRequest;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeResult;
import com.templateai.sandbox.transaction.TransactionDtos.TransactionResponse;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactions;
    private final CardService cardService;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, CardService cardService, Clock clock) {
        this.transactions = transactions;
        this.cardService = cardService;
        this.clock = clock;
    }

    /**
     * Authorize a spend against a card.
     *
     * <p>A rejected authorization is a stored DECLINED row, not an error response: the caller's
     * request was valid and the answer is "no". Only unusable input (unknown card, wrong currency)
     * is a 4xx.
     */
    public AuthorizeResult authorize(AuthorizeRequest request, String idempotencyKey) {
        // Replay of a key we have already answered: return the original outcome, charge nothing.
        // A concurrent duplicate loses the unique index race and surfaces as 409 — retry-safe,
        // and the same thing Stripe does for an in-flight key.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Transaction> replay = transactions.findByIdempotencyKey(idempotencyKey);
            if (replay.isPresent()) {
                return new AuthorizeResult(TransactionResponse.from(replay.get()), true);
            }
        }

        // Locks the card row for the rest of this transaction. Everything below — reading spend,
        // deciding, writing the result — has to be one atomic step, or two concurrent charges both
        // read the same total and both approve past the limit.
        Card card = cardService.requireForUpdate(request.cardId());
        if (!card.getCurrency().equals(request.currency())) {
            throw ApiException.badRequest(
                    "Currency mismatch: card %d is denominated in %s".formatted(card.getId(), card.getCurrency()));
        }

        Transaction tx = new Transaction();
        tx.setCard(card);
        tx.setAmountMinor(request.amountMinor());
        tx.setCurrency(request.currency());
        tx.setMerchant(request.merchant());
        tx.setIdempotencyKey(emptyToNull(idempotencyKey));
        tx.setCreatedAt(Instant.now(clock));

        Transaction.DeclineReason decline = declineReason(card, request.amountMinor());
        tx.setStatus(decline == null ? Transaction.Status.APPROVED : Transaction.Status.DECLINED);
        tx.setDeclineReason(decline);

        return new AuthorizeResult(TransactionResponse.from(transactions.save(tx)), false);
    }

    /**
     * Available balance is derived by summing approved spend rather than kept in a mutable column,
     * so it cannot drift out of sync with the transactions that produced it. At real volume you'd
     * maintain a running balance and reconcile against this sum — see docs/SYSTEM-DESIGN.md.
     *
     * <p>Callers must already hold the card's row lock; see {@link #authorize}.
     */
    private Transaction.DeclineReason declineReason(Card card, long amountMinor) {
        if (card.getStatus() == Card.Status.FROZEN) {
            return Transaction.DeclineReason.CARD_FROZEN;
        }
        if (card.getStatus() == Card.Status.CANCELLED) {
            return Transaction.DeclineReason.CARD_CANCELLED;
        }
        long spent = transactions.sumAmountByCardAndStatus(card.getId(), Transaction.Status.APPROVED);
        if (spent + amountMinor > card.getSpendLimitMinor()) {
            return Transaction.DeclineReason.LIMIT_EXCEEDED;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(Long cardId, Pageable pageable) {
        Page<Transaction> page = cardId == null
                ? transactions.findAllByOrderByCreatedAtDesc(pageable)
                : transactions.findByCardIdOrderByCreatedAtDesc(cardId, pageable);
        return PageResponse.of(page, TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        return transactions.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> ApiException.notFound("Transaction " + id + " not found"));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
