package com.templateai.sandbox;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.templateai.sandbox.task.Task;
import com.templateai.sandbox.task.TaskDtos.CreateTaskRequest;
import com.templateai.sandbox.task.TaskDtos.TaskResponse;
import com.templateai.sandbox.task.TaskDtos.UpdateTaskRequest;
import com.templateai.sandbox.task.TaskService;

/**
 * Seeds the in-memory database so the UI has something to show the moment it loads. Runs on the
 * {@code h2} profile only — never in tests, which assert on rows they created themselves.
 *
 * <p>Goes through the service rather than the repository, so the seed exercises the same rules a
 * real request would.
 */
@Component
@Profile("h2")
public class DemoData implements CommandLineRunner {

    private final TaskService tasks;

    public DemoData(TaskService tasks) {
        this.tasks = tasks;
    }

    @Override
    public void run(String... args) {
        if (!tasks.list(null).isEmpty()) {
            return;
        }
        seed("Agree the feature list", "Write down what we are building before any code.", Task.Status.DONE);
        seed("Build the first vertical slice", "Entity, repository, service, controller, UI.", Task.Status.IN_PROGRESS);
        seed("Cover the rule that matters with a test", "Happy path plus the one rule.", Task.Status.TODO);
        seed("Rehearse the demo", "Run the exact sequence once before showing it.", Task.Status.TODO);
    }

    private void seed(String title, String description, Task.Status status) {
        TaskResponse created = tasks.create(new CreateTaskRequest(title, description));
        if (status != Task.Status.TODO) {
            tasks.update(created.id(), new UpdateTaskRequest(null, null, status));
        }
    }
}
