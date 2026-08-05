package com.templateai.sandbox.task;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The sample domain. It exists to prove the database, API, validation, errors, UI, and tests are
 * wired together — nothing more. Rename or delete this package when the real problem arrives.
 */
@Entity
@Table(name = "tasks", indexes = @Index(name = "ix_tasks_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
public class Task {

    public enum Status { TODO, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /** STRING, never ordinal: reordering the enum must not rewrite the meaning of stored rows. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.TODO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
