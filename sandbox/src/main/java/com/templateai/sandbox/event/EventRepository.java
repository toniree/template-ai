package com.templateai.sandbox.event;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Search happens in SQL: both filters are optional (null skips that condition), matched
     * case-insensitively against title/artist/venue for {@code q} and location separately.
     */
    @Query("""
            select e from Event e
            where (:q is null
                or lower(e.title) like lower(concat('%', :q, '%'))
                or lower(e.artistName) like lower(concat('%', :q, '%'))
                or lower(e.venueName) like lower(concat('%', :q, '%')))
              and (:location is null or lower(e.location) like lower(concat('%', :location, '%')))
            order by e.startTime asc
            """)
    List<Event> search(@Param("q") String q, @Param("location") String location);
}
