package com.templateai.sandbox.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Artist and venue are attributes of an event, not separate entities — nothing in the stated
 * requirements (search/view events, book tickets) manages venues or artists on their own, so a
 * Venue/Artist table would be unused scope.
 */
@Entity
@Table(name = "events", indexes = @Index(name = "ix_events_start_time", columnList = "start_time"))
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;
}
