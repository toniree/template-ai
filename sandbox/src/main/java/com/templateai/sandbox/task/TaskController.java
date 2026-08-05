package com.templateai.sandbox.task;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.templateai.sandbox.task.TaskDtos.CreateTaskRequest;
import com.templateai.sandbox.task.TaskDtos.TaskResponse;
import com.templateai.sandbox.task.TaskDtos.UpdateTaskRequest;

/**
 * HTTP only: status codes, headers, param binding. No business rules, no try/catch —
 * {@code GlobalExceptionHandler} owns every error response.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    /** An unparseable {@code status} binds to nothing and comes back as a 400, not a 500. */
    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) Task.Status status) {
        return taskService.list(status);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    /** PATCH, not PUT: this updates the fields it was given, it does not replace the task. */
    @PatchMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
