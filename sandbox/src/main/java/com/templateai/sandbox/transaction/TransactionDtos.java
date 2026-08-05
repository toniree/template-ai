package com.templateai.sandbox.transaction;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    /** Note: the idempotency key is an {@code Idempotency-Key} header, not a body field. */
    public record AuthorizeRequest(
            @NotNull Long cardId,
            @Positive long amountMinor,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO-4217 code") String currency,
            @NotBlank String merchant
    ) {
    }

    public record TransactionResponse(
            Long id,
            Long cardId,
            long amountMinor,
            String currency,
            String merchant,
            Transaction.Status status,
            Transaction.DeclineReason declineReason,
            Instant createdAt
    ) {

        public static TransactionResponse from(Transaction tx) {
            return new TransactionResponse(
                    tx.getId(),
                    tx.getCard().getId(),
                    tx.getAmountMinor(),
                    tx.getCurrency(),
                    tx.getMerchant(),
                    tx.getStatus(),
                    tx.getDeclineReason(),
                    tx.getCreatedAt());
        }
    }

    /**
     * {@code replayed} lets the controller answer 200 for a repeated Idempotency-Key and 201 for a
     * genuinely new authorization, without the service knowing anything about HTTP.
     */
    public record AuthorizeResult(TransactionResponse transaction, boolean replayed) {
    }
}
