package com.templateai.sandbox.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.ticket.TicketDtos.BookTicketRequest;
import com.templateai.sandbox.ticket.TicketDtos.TicketResponse;
import com.templateai.sandbox.user.UserService;

@Service
@Transactional
public class TicketService {

    private final TicketRepository tickets;
    private final UserService userService;
    private final Clock clock;

    public TicketService(TicketRepository tickets, UserService userService, Clock clock) {
        this.tickets = tickets;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForEvent(Long eventId, Ticket.Status status) {
        List<Ticket> found = status == null
                ? tickets.findByEventIdOrderByIdAsc(eventId)
                : tickets.findByEventIdAndStatusOrderByIdAsc(eventId, status);
        return found.stream().map(TicketResponse::from).toList();
    }

    /** Backs the "My tickets" screen: every ticket the given user has booked, across all events. */
    @Transactional(readOnly = true)
    public List<TicketResponse> listMine(Long ownerId) {
        userService.find(ownerId);
        return tickets.findByOwnerIdOrderByBookedAtDesc(ownerId).stream().map(TicketResponse::from).toList();
    }

    /**
     * Strong consistency for booking: the existence check below is only for a precise 404 message.
     * The invariant itself — a ticket books once — is enforced by the conditional {@code UPDATE} in
     * {@link TicketRepository#book}, which is atomic and needs no lock.
     */
    public TicketResponse book(Long id, Long ownerId, BookTicketRequest request) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket " + id + " not found"));
        userService.find(ownerId);

        int updated = tickets.book(id, request.buyerName(), request.buyerEmail(), Instant.now(clock), ownerId);
        if (updated == 0) {
            throw ApiException.conflict("Ticket " + id + " is already booked");
        }
        return TicketResponse.from(tickets.findById(ticket.getId()).orElseThrow());
    }
}
