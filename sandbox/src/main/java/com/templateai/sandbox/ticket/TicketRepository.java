package com.templateai.sandbox.ticket;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEventIdOrderByIdAsc(Long eventId);

    List<Ticket> findByEventIdAndStatusOrderByIdAsc(Long eventId, Ticket.Status status);

    long countByEventIdAndStatus(Long eventId, Ticket.Status status);

    /**
     * Atomic write, not read-then-write: the {@code where status = 'AVAILABLE'} makes the check and
     * the write one round trip, so two concurrent bookings for the same seat can't both succeed.
     * Rows-affected of 0 means the invariant already didn't hold — the caller turns that into 409.
     */
    /**
     * {@code clearAutomatically}: a bulk JPQL update bypasses the persistence context, so without
     * this the entity loaded earlier in the same transaction would keep reporting its stale,
     * pre-update status.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Ticket t set t.status = 'BOOKED', t.buyerName = :buyerName, t.buyerEmail = :buyerEmail,
                t.bookedAt = :bookedAt, t.owner.id = :ownerId
            where t.id = :id and t.status = 'AVAILABLE'
            """)
    int book(@Param("id") Long id, @Param("buyerName") String buyerName, @Param("buyerEmail") String buyerEmail,
            @Param("bookedAt") Instant bookedAt, @Param("ownerId") Long ownerId);

    /** Backs "my tickets": every ticket a given user has booked, most recent first. */
    List<Ticket> findByOwnerIdOrderByBookedAtDesc(Long ownerId);
}
