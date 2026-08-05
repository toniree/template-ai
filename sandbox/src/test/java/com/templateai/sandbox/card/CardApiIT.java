package com.templateai.sandbox.card;

import static org.hamcrest.Matchers.is;
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

import com.templateai.sandbox.card.CardDtos.CreateCardRequest;
import com.templateai.sandbox.card.CardDtos.UpdateCardRequest;

/**
 * Template for a resource integration test: real HTTP shape, real database, no mocks.
 * Copy this file when you add a resource and change the payloads.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createsThenReadsBackACard() throws Exception {
        String location = mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateCardRequest("Ada Lovelace", "4242", 250_000L, "USD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.availableMinor", is(250_000)))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last4", is("4242")))
                .andExpect(jsonPath("$.spentMinor", is(0)));
    }

    @Test
    void freezesACard() throws Exception {
        String location = createCard();

        mockMvc.perform(patch(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UpdateCardRequest(Card.Status.FROZEN, 250_000L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FROZEN")));
    }

    @Test
    void rejectsAnInvalidPayloadWithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CreateCardRequest("", "42", -1L, "dollars"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details.length()", is(4)));
    }

    @Test
    void returnsNotFoundForAnUnknownCard() throws Exception {
        mockMvc.perform(get("/api/cards/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private String createCard() throws Exception {
        return mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateCardRequest("Grace Hopper", "1881", 250_000L, "USD"))))
                .andReturn().getResponse().getHeader("Location");
    }
}
