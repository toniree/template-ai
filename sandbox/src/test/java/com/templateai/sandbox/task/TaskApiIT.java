package com.templateai.sandbox.task;

import static com.templateai.sandbox.support.ApiErrors.created;
import static com.templateai.sandbox.support.ApiErrors.notFound;
import static com.templateai.sandbox.support.ApiErrors.validationError;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.templateai.sandbox.support.ApiIntegrationTest;
import com.templateai.sandbox.task.TaskDtos.CreateTaskRequest;

/**
 * The template for a resource integration test: real HTTP shape, real database, no mocks. Copy this
 * file when you add a feature and change the payloads.
 *
 * <p>Extends {@link ApiIntegrationTest}, which supplies {@code http}, {@code json}, the {@code test}
 * profile, {@code postJson}/{@code patchJson}, and {@code as(userId)} for calling as a principal.
 * Error outcomes are asserted with {@code ApiErrors} so the whole error body is checked, not just
 * the status line.
 *
 * <p>The suite shares one database (real PostgreSQL, {@code sandbox_test}, on this branch), so
 * every test creates its own rows and asserts on those — never on table-wide counts, which break
 * the moment another test runs alongside.
 */
class TaskApiIT extends ApiIntegrationTest {

    @Test
    void createsThenReadsBackATask() throws Exception {
        String location = http.perform(postJson("/api/tasks",
                        new CreateTaskRequest("Write the runbook", "One page, no more.")))
                .andExpect(created())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andReturn().getResponse().getHeader("Location");

        http.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Write the runbook")))
                .andExpect(jsonPath("$.description", is("One page, no more.")));
    }

    @Test
    void movesATaskThroughItsStatuses() throws Exception {
        String location = createTask("Ship the slice");

        http.perform(patchJson(location, """
                        {"status":"IN_PROGRESS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        http.perform(patchJson(location, """
                        {"status":"DONE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    /** The rule that matters on a PATCH: it must not blank the fields it doesn't mention. */
    @Test
    void patchingOnlyTheStatusLeavesTheTitleAndDescriptionIntact() throws Exception {
        String location = createTask("Keep this title");

        http.perform(patchJson(location, """
                        {"status":"DONE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.title", is("Keep this title")))
                .andExpect(jsonPath("$.description", is("Seeded by the test")));
    }

    /** Filtering happens in SQL; the response must contain only rows in the requested status. */
    @Test
    void filtersTheListByStatus() throws Exception {
        String done = createTask("Filter fixture");
        http.perform(patchJson(done, """
                {"status":"DONE"}"""));

        http.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status != 'DONE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.title == 'Filter fixture')]").exists());
    }

    /**
     * A PATCH may omit the title entirely, but if it sends one it has to contain something.
     * {@code @Size(min = 1)} counts characters and would let this through.
     */
    @Test
    void rejectsAPatchWhoseTitleIsOnlyWhitespace() throws Exception {
        String location = createTask("Real title");

        http.perform(patchJson(location, """
                        {"title":"   "}"""))
                .andExpect(validationError("title must not be blank"));

        // ...and the stored title is untouched.
        http.perform(get(location)).andExpect(jsonPath("$.title", is("Real title")));
    }

    @Test
    void rejectsACreateWithABlankTitle() throws Exception {
        http.perform(postJson("/api/tasks", """
                        {"title":"","description":"no title"}"""))
                .andExpect(validationError("title must not be blank"));
    }

    @Test
    void deletesATaskAndThenCannotFindIt() throws Exception {
        String location = createTask("Temporary");

        http.perform(delete(location)).andExpect(status().isNoContent());
        http.perform(get(location)).andExpect(notFound());
        http.perform(delete(location)).andExpect(notFound());
    }

    @Test
    void returnsNotFoundForAnUnknownTask() throws Exception {
        http.perform(get("/api/tasks/999999")).andExpect(notFound());
    }

    private String createTask(String title) throws Exception {
        return http.perform(postJson("/api/tasks", new CreateTaskRequest(title, "Seeded by the test")))
                .andExpect(created())
                .andReturn().getResponse().getHeader("Location");
    }
}
