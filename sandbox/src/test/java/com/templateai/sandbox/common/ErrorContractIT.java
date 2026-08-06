package com.templateai.sandbox.common;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bad requests must come back as 4xx. A 500 tells the caller "our fault, retry" — which is wrong,
 * and sends them into a loop that can never succeed.
 *
 * <p>These all returned 500 before {@code GlobalExceptionHandler} extended
 * {@code ResponseEntityExceptionHandler}: the catch-all was intercepting Spring MVC's own
 * exceptions before their correct status codes could be applied. Keep these tests when you delete
 * the sample domain — just repoint the paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorContractIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unparseablePathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid value for 'id'")));
    }

    @Test
    void unparseableQueryParameterIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/1/tickets").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid value for 'status'")));
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(get("/api/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(put("/api/tickets/1/book").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tickets/1/book")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Malformed request body")));
    }
}
