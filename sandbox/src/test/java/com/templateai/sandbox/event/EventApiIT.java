package com.templateai.sandbox.event;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.templateai.sandbox.ticket.Ticket;
import com.templateai.sandbox.ticket.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository events;

    @Autowired
    private TicketRepository tickets;

    @Test
    void searchMatchesByArtistNameCaseInsensitively() throws Exception {
        seedEvent("The Eras Tour", "Taylor Swift", "SoFi Stadium", "Inglewood, CA");

        mockMvc.perform(get("/api/events").param("q", "taylor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'The Eras Tour')]").exists());

        mockMvc.perform(get("/api/events").param("q", "nonexistent-artist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'The Eras Tour')]").doesNotExist());
    }

    @Test
    void searchFiltersByLocationIndependentlyOfQ() throws Exception {
        seedEvent("Late Night Sessions", "Norah Jones", "Blue Note", "New York, NY");

        mockMvc.perform(get("/api/events").param("location", "new york"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Late Night Sessions')]").exists());
    }

    @Test
    void detailReportsAvailableTicketCount() throws Exception {
        Long eventId = seedEvent("Music of the Spheres", "Coldplay", "MetLife Stadium", "East Rutherford, NJ");
        seedTicket(eventId);
        Long booked = seedTicket(eventId);
        booked(booked);

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTicketCount", is(1)));
    }

    @Test
    void returnsNotFoundForAnUnknownEvent() throws Exception {
        mockMvc.perform(get("/api/events/999999")).andExpect(status().isNotFound());
    }

    private Long seedEvent(String title, String artistName, String venueName, String location) {
        Event event = new Event();
        event.setTitle(title);
        event.setArtistName(artistName);
        event.setVenueName(venueName);
        event.setLocation(location);
        event.setStartTime(Instant.now());
        return events.save(event).getId();
    }

    private Long seedTicket(Long eventId) {
        Ticket ticket = new Ticket();
        ticket.setEvent(events.getReferenceById(eventId));
        ticket.setSection("Floor");
        ticket.setSeatLabel("Floor 1");
        ticket.setPriceCents(10000);
        return tickets.save(ticket).getId();
    }

    private void booked(Long ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow();
        ticket.setStatus(Ticket.Status.BOOKED);
        tickets.save(ticket);
    }
}
