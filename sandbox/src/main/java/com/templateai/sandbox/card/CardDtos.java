package com.templateai.sandbox.card;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request/response records for the card API, grouped in one file so adding a feature is 4 files,
 * not 8. Entities never cross the controller boundary; these do.
 */
public final class CardDtos {

    private CardDtos() {
    }

    public record CreateCardRequest(
            @NotBlank String cardholderName,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String last4,
            @PositiveOrZero long spendLimitMinor,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO-4217 code") String currency
    ) {
    }

    /** Partial update: the only two fields an operator can change on a live card. */
    public record UpdateCardRequest(
            @NotNull Card.Status status,
            @PositiveOrZero long spendLimitMinor
    ) {
    }

    public record CardResponse(
            Long id,
            String cardholderName,
            String last4,
            Card.Status status,
            long spendLimitMinor,
            long spentMinor,
            long availableMinor,
            String currency,
            Instant createdAt
    ) {

        /** Static factory beats a mapper class or MapStruct at this size. */
        public static CardResponse from(Card card, long spentMinor) {
            return new CardResponse(
                    card.getId(),
                    card.getCardholderName(),
                    card.getLast4(),
                    card.getStatus(),
                    card.getSpendLimitMinor(),
                    spentMinor,
                    card.getSpendLimitMinor() - spentMinor,
                    card.getCurrency(),
                    card.getCreatedAt());
        }
    }
}
