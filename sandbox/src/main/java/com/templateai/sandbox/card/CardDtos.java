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

    /**
     * Boxed {@code Long}, not {@code long}: a primitive silently defaults to 0 when the caller
     * omits the field, and {@code @PositiveOrZero} would happily accept that — creating a card
     * with no spend limit instead of rejecting an incomplete request.
     */
    public record CreateCardRequest(
            @NotBlank String cardholderName,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String last4,
            @NotNull @PositiveOrZero Long spendLimitMinor,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO-4217 code") String currency
    ) {
    }

    /**
     * Genuinely partial: null means "leave it alone", so freezing a card doesn't require the caller
     * to know and resend its current limit. Both fields boxed for the same reason as above — with a
     * primitive, {@code {"status":"FROZEN"}} would zero the spend limit as a side effect.
     */
    public record UpdateCardRequest(
            Card.Status status,
            @PositiveOrZero Long spendLimitMinor
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
