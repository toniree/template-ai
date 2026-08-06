package com.templateai.sandbox.event;

import java.time.Instant;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventResponse(
            Long id,
            String title,
            String artistName,
            String venueName,
            String location,
            Instant startTime
    ) {

        public static EventResponse from(Event event) {
            return new EventResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getArtistName(),
                    event.getVenueName(),
                    event.getLocation(),
                    event.getStartTime());
        }
    }

    /** Detail view adds the one aggregate the list doesn't need: how many seats are still up for grabs. */
    public record EventDetailResponse(
            Long id,
            String title,
            String artistName,
            String venueName,
            String location,
            Instant startTime,
            long availableTicketCount
    ) {

        public static EventDetailResponse from(Event event, long availableTicketCount) {
            return new EventDetailResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getArtistName(),
                    event.getVenueName(),
                    event.getLocation(),
                    event.getStartTime(),
                    availableTicketCount);
        }
    }
}
