package com.templateai.sandbox.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Derived query — Spring Data writes the SQL. Filtering happens in the database, never by
     * loading every row and filtering in Java. {@code findAll(Sort)} covers the unfiltered case,
     * so that one needs no method here.
     */
    List<Task> findByStatusOrderByIdAsc(Task.Status status);
}
