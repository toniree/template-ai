package com.templateai.sandbox.ticket;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.templateai.sandbox.event.Event;
import com.templateai.sandbox.event.EventRepository;
import com.templateai.sandbox.ticket.TicketDtos.BookTicketRequest;
import com.templateai.sandbox.user.User;
import com.templateai.sandbox.user.UserRepository;

/** Real HTTP shape, real database, no mocks — the suite shares one H2 database, so every test creates its own rows. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private EventRepository events;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private UserRepository users;

    @Test
    void booksAnAvailableTicket() throws Exception {
        Long ticketId = seedTicket();
        Long userId = seedUser();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/book")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BookTicketRequest("Ada Lovelace", "ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("BOOKED")))
                .andExpect(jsonPath("$.buyerName", is("Ada Lovelace")));
    }

    /** The rule that matters: a ticket can only ever be booked once. */
    @Test
    void rejectsBookingATicketThatIsAlreadyBooked() throws Exception {
        Long ticketId = seedTicket();
        Long userId = seedUser();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/book")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new BookTicketRequest("Ada Lovelace", "ada@example.com"))));

        mockMvc.perform(post("/api/tickets/" + ticketId + "/book")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BookTicketRequest("Grace Hopper", "grace@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void returnsNotFoundForAnUnknownTicket() throws Exception {
        mockMvc.perform(post("/api/tickets/999999/book")
                        .header("X-User-Id", seedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BookTicketRequest("Ada Lovelace", "ada@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBookingWithoutAnEmail() throws Exception {
        Long ticketId = seedTicket();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/book")
                        .header("X-User-Id", seedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerName":"Ada Lovelace"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    void listsOnlyAvailableTicketsForAnEvent() throws Exception {
        Long eventId = seedEvent();
        Long available = seedTicket(eventId);
        Long booked = seedTicket(eventId);
        mockMvc.perform(post("/api/tickets/" + booked + "/book")
                .header("X-User-Id", seedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new BookTicketRequest("Ada Lovelace", "ada@example.com"))));

        mockMvc.perform(get("/api/events/" + eventId + "/tickets").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + available + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + booked + ")]").doesNotExist());
    }

    /** The rule that matters here: "my tickets" is scoped to the requesting user, not every booking. */
    @Test
    void listsOnlyTheRequestingUsersTickets() throws Exception {
        Long mine = seedTicket();
        Long someoneElses = seedTicket();
        Long userId = seedUser();
        Long otherUserId = seedUser();

        mockMvc.perform(post("/api/tickets/" + mine + "/book")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new BookTicketRequest("Ada Lovelace", "ada@example.com"))));
        mockMvc.perform(post("/api/tickets/" + someoneElses + "/book")
                .header("X-User-Id", otherUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new BookTicketRequest("Grace Hopper", "grace@example.com"))));

        mockMvc.perform(get("/api/tickets/mine").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + mine + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + someoneElses + ")]").doesNotExist());
    }

    private Long seedUser() {
        User user = new User();
        user.setName("Ada Lovelace");
        user.setEmail("ada@example.com");
        return users.save(user).getId();
    }

    private Long seedEvent() {
        Event event = new Event();
        event.setTitle("Test Concert");
        event.setArtistName("Test Artist");
        event.setVenueName("Test Venue");
        event.setLocation("Test City");
        event.setStartTime(Instant.now());
        return events.save(event).getId();
    }

    private Long seedTicket() {
        return seedTicket(seedEvent());
    }

    private Long seedTicket(Long eventId) {
        Ticket ticket = new Ticket();
        ticket.setEvent(events.getReferenceById(eventId));
        ticket.setSection("Floor");
        ticket.setSeatLabel("Floor 1");
        ticket.setPriceCents(10000);
        return tickets.save(ticket).getId();
    }
}
