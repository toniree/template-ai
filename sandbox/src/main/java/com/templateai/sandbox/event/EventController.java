package com.templateai.sandbox.event;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.templateai.sandbox.event.EventDtos.EventDetailResponse;
import com.templateai.sandbox.event.EventDtos.EventResponse;
import com.templateai.sandbox.ticket.Ticket;
import com.templateai.sandbox.ticket.TicketDtos.TicketResponse;
import com.templateai.sandbox.ticket.TicketService;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final TicketService ticketService;

    public EventController(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<EventResponse> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String location) {
        return eventService.search(q, location);
    }

    @GetMapping("/{id}")
    public EventDetailResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    /** Ticket picker for the booking flow: filter to AVAILABLE to show only seats worth showing. */
    @GetMapping("/{id}/tickets")
    public List<TicketResponse> tickets(@PathVariable Long id, @RequestParam(required = false) Ticket.Status status) {
        return ticketService.listForEvent(id, status);
    }
}
