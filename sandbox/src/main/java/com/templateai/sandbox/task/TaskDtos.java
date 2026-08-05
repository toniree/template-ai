package com.templateai.sandbox.task;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Every request/response record for the feature in one file, so adding a feature is 5 files rather
 * than 10. Entities never cross the controller boundary; these do.
 */
public final class TaskDtos {

    private TaskDtos() {
    }

    /** New tasks always start as TODO — status is moved by a PATCH, not chosen at creation. */
    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description
    ) {
    }

    /**
     * Genuinely partial: null means "leave it alone", so moving a task to DONE doesn't require the
     * caller to know and resend its current title.
     *
     * <p>Title needs two different rules at once, which is why it carries two annotations.
     * {@code @NotBlank} is wrong here — it rejects null, and null is how the caller says "don't
     * touch this". {@code @Size(min = 1)} is also wrong: it counts characters, so {@code "   "}
     * passes and blanks the title anyway. Both constraints skip null, so the pattern is what
     * enforces "if you sent a title, it has to contain something".
     *
     * <p>Any numeric or boolean field added here must be boxed ({@code Long}, not {@code long}) —
     * a primitive silently becomes 0/false when the caller omits it, and validation accepts that.
     */
    public record UpdateTaskRequest(
            @Size(max = 200) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String title,
            @Size(max = 2000) String description,
            Task.Status status
    ) {
    }

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Task.Status status,
            Instant createdAt,
            Instant updatedAt
    ) {

        /** A static factory beats a mapper class or MapStruct at this size. */
        public static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getCreatedAt(),
                    task.getUpdatedAt());
        }
    }
}
