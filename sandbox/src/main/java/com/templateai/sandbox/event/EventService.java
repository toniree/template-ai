package com.templateai.sandbox.event;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.event.EventDtos.EventDetailResponse;
import com.templateai.sandbox.event.EventDtos.EventResponse;
import com.templateai.sandbox.ticket.Ticket;
import com.templateai.sandbox.ticket.TicketRepository;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository events;
    private final TicketRepository tickets;

    public EventService(EventRepository events, TicketRepository tickets) {
        this.events = events;
        this.tickets = tickets;
    }

    /** {@code q} and {@code location} are both optional; filtering happens in SQL either way. */
    public List<EventResponse> search(String q, String location) {
        return events.search(q, location).stream().map(EventResponse::from).toList();
    }

    public EventDetailResponse get(Long id) {
        Event event = events.findById(id).orElseThrow(() -> ApiException.notFound("Event " + id + " not found"));
        long available = tickets.countByEventIdAndStatus(id, Ticket.Status.AVAILABLE);
        return EventDetailResponse.from(event, available);
    }
}
