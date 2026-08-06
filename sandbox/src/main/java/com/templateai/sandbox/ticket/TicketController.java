package com.templateai.sandbox.ticket;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.templateai.sandbox.ticket.TicketDtos.BookTicketRequest;
import com.templateai.sandbox.ticket.TicketDtos.TicketResponse;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Booking is answered directly by the service call; no separate reserve/confirm hold.
     * {@code X-User-Id} carries the statically-selected "current user" — see {@code user} package.
     */
    @PostMapping("/{id}/book")
    public TicketResponse book(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BookTicketRequest request) {
        return ticketService.book(id, userId, request);
    }

    /** Backs the "My tickets" screen. */
    @GetMapping("/mine")
    public List<TicketResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return ticketService.listMine(userId);
    }
}
