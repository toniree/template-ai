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

    /** A partial PATCH must not zero the fields it doesn't mention. */
    @Test
    void patchingOnlyTheStatusLeavesTheSpendLimitIntact() throws Exception {
        String location = createCard();

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FROZEN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FROZEN")))
                .andExpect(jsonPath("$.spendLimitMinor", is(250_000)));
    }

    @Test
    void patchingOnlyTheSpendLimitLeavesTheStatusIntact() throws Exception {
        String location = createCard();

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spendLimitMinor":5000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.spendLimitMinor", is(5000)));
    }

    /** Omitting a required amount must be rejected, not silently treated as zero. */
    @Test
    void rejectsACreateThatOmitsTheSpendLimit() throws Exception {
        mockMvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardholderName":"Ada","last4":"4242","currency":"USD"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("spendLimitMinor must not be null")));
    }

    /** ACTIVE <-> FROZEN both ways, either to CANCELLED, and CANCELLED is the end. */
    @Test
    void cancellationIsTerminal() throws Exception {
        String location = createCard();

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Cannot be revived...
        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isConflict());

        // ...and its limit is frozen too, not just its status.
        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spendLimitMinor":999}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(get(location)).andExpect(jsonPath("$.spendLimitMinor", is(250_000)));
    }

    @Test
    void aFrozenCardCanBeReactivated() throws Exception {
        String location = createCard();

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FROZEN"}"""))
                .andExpect(jsonPath("$.status", is("FROZEN")));

        mockMvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
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
