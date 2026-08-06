package com.templateai.sandbox.ticket;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TicketDtos {

    private TicketDtos() {
    }

    public record BookTicketRequest(
            @NotBlank @Size(max = 200) String buyerName,
            @NotBlank @Email @Size(max = 200) String buyerEmail
    ) {
    }

    public record TicketResponse(
            Long id,
            Long eventId,
            String eventTitle,
            String section,
            String seatLabel,
            Integer priceCents,
            Ticket.Status status,
            String buyerName,
            Instant bookedAt
    ) {

        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getEvent().getId(),
                    ticket.getEvent().getTitle(),
                    ticket.getSection(),
                    ticket.getSeatLabel(),
                    ticket.getPriceCents(),
                    ticket.getStatus(),
                    ticket.getBuyerName(),
                    ticket.getBookedAt());
        }
    }
}
