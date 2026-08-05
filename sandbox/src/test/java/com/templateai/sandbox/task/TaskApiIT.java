package com.templateai.sandbox.task;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.templateai.sandbox.task.TaskDtos.CreateTaskRequest;

/**
 * The template for a resource integration test: real HTTP shape, real database, no mocks. Copy this
 * file when you add a feature and change the payloads.
 *
 * <p>The suite shares one H2 database, so every test creates its own rows and asserts on those —
 * never on table-wide counts, which break the moment another test runs alongside.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createsThenReadsBackATask() throws Exception {
        String location = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateTaskRequest("Write the runbook", "One page, no more."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Write the runbook")))
                .andExpect(jsonPath("$.description", is("One page, no more.")));
    }

    @Test
    void movesATaskThroughItsStatuses() throws Exception {
        String location = createTask("Ship the slice");

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    /** The rule that matters on a PATCH: it must not blank the fields it doesn't mention. */
    @Test
    void patchingOnlyTheStatusLeavesTheTitleAndDescriptionIntact() throws Exception {
        String location = createTask("Keep this title");

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
        mockMvc.perform(patch(done).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"DONE"}"""));

        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
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

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details[0]", is("title must not be blank")));

        // ...and the stored title is untouched.
        mockMvc.perform(get(location)).andExpect(jsonPath("$.title", is("Real title")));
    }

    @Test
    void rejectsACreateWithABlankTitle() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"no title"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details[0]", is("title must not be blank")));
    }

    @Test
    void deletesATaskAndThenCannotFindIt() throws Exception {
        String location = createTask("Temporary");

        mockMvc.perform(delete(location)).andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
        mockMvc.perform(delete(location)).andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForAnUnknownTask() throws Exception {
        mockMvc.perform(get("/api/tasks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private String createTask(String title) throws Exception {
        return mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CreateTaskRequest(title, "Seeded by the test"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
