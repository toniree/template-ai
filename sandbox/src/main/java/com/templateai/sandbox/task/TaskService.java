package com.templateai.sandbox.task;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.task.TaskDtos.CreateTaskRequest;
import com.templateai.sandbox.task.TaskDtos.TaskResponse;
import com.templateai.sandbox.task.TaskDtos.UpdateTaskRequest;

/**
 * Every business rule lives here. The controller does HTTP, the repository does queries, and
 * entities never escape this layer — {@code open-in-view} is off, so mapping to a response record
 * has to happen inside the transaction.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final Clock clock;

    public TaskService(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    public TaskResponse create(CreateTaskRequest request) {
        Instant now = Instant.now(clock);

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(Task.Status.TODO);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return TaskResponse.from(tasks.save(task));
    }

    /** {@code status} is optional — null means "all". Filtering happens in SQL either way. */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(Task.Status status) {
        List<Task> found = status == null
                ? tasks.findAll(Sort.by(Sort.Direction.ASC, "id"))
                : tasks.findByStatusOrderByIdAsc(status);
        return found.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(require(id));
    }

    /**
     * Partial update: only the fields the caller actually sent are applied. Any status may follow
     * any other — this sample deliberately has no state machine. If a problem needs one, put the
     * check in a single private method here rather than scattering ifs.
     */
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = require(id);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.setUpdatedAt(Instant.now(clock));
        return TaskResponse.from(task);
    }

    public void delete(Long id) {
        // Check first so deleting an unknown id is a 404, not a silent 204 that hides a client bug.
        tasks.delete(require(id));
    }

    /** Shared 404 so every "unknown task" path returns the identical error body. */
    private Task require(Long id) {
        return tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task " + id + " not found"));
    }
}
