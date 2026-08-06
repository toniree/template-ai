package com.templateai.sandbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.event.Event;
import com.templateai.sandbox.event.EventRepository;
import com.templateai.sandbox.ticket.Ticket;
import com.templateai.sandbox.ticket.TicketRepository;
import com.templateai.sandbox.user.User;
import com.templateai.sandbox.user.UserRepository;

/**
 * Seeds the in-memory database so the UI has something to show the moment it loads. Runs on the
 * {@code h2} profile only — never in tests, which assert on rows they created themselves.
 *
 * <p>Writes through the repositories directly rather than the services: there is no business rule
 * at creation time (no create endpoint exists — events/tickets are fixture data, not something the
 * stated requirements ask users to author), so going through a service would add a method nothing
 * else calls.
 */
@Component
@Profile("h2")
public class DemoData implements CommandLineRunner {

    private final EventRepository events;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final Clock clock;

    public DemoData(EventRepository events, TicketRepository tickets, UserRepository users, Clock clock) {
        this.events = events;
        this.tickets = tickets;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!events.findAll().isEmpty()) {
            return;
        }

        user("Ava Chen", "ava@example.com");
        user("Marcus Reyes", "marcus@example.com");
        user("Priya Nair", "priya@example.com");

        Event taylor = event("The Eras Tour", "Taylor Swift", "SoFi Stadium", "Inglewood, CA", 14);
        seatBlock(taylor, "Floor", 4, 45000);
        seatBlock(taylor, "Lower Bowl", 12, 25000);
        seatBlock(taylor, "Upper Bowl", 20, 9500);

        Event coldplay = event("Music of the Spheres", "Coldplay", "MetLife Stadium", "East Rutherford, NJ", 30);
        seatBlock(coldplay, "Floor", 6, 32000);
        seatBlock(coldplay, "Lower Bowl", 15, 18000);

        Event jazz = event("Late Night Sessions", "Norah Jones", "Blue Note", "New York, NY", 3);
        seatBlock(jazz, "Table", 10, 12000);
    }

    private void user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        users.save(user);
    }

    private Event event(String title, String artistName, String venueName, String location, int daysFromNow) {
        Event event = new Event();
        event.setTitle(title);
        event.setArtistName(artistName);
        event.setVenueName(venueName);
        event.setLocation(location);
        event.setStartTime(Instant.now(clock).plus(Duration.ofDays(daysFromNow)));
        return events.save(event);
    }

    private void seatBlock(Event event, String section, int seatCount, int priceCents) {
        for (int i = 1; i <= seatCount; i++) {
            Ticket ticket = new Ticket();
            ticket.setEvent(event);
            ticket.setSection(section);
            ticket.setSeatLabel(section + " " + i);
            ticket.setPriceCents(priceCents);
            tickets.save(ticket);
        }
    }
}
