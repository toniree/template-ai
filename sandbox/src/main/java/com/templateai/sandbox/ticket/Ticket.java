package com.templateai.sandbox.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.templateai.sandbox.event.Event;
import com.templateai.sandbox.user.User;

@Entity
@Table(name = "tickets", indexes = @Index(name = "ix_tickets_event_status", columnList = "event_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    public enum Status { AVAILABLE, BOOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 40)
    private String section;

    @Column(name = "seat_label", nullable = false, length = 40)
    private String seatLabel;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.AVAILABLE;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "buyer_email", length = 200)
    private String buyerEmail;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
