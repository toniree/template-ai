package com.templateai.sandbox.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.templateai.sandbox.card.Card;
import com.templateai.sandbox.card.CardDtos.CreateCardRequest;
import com.templateai.sandbox.card.CardDtos.UpdateCardRequest;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeRequest;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeResult;

/**
 * The rules that actually matter on a spend endpoint: limits are enforced, declines are recorded
 * rather than discarded, and a retried request never charges twice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private TransactionService transactionService;

    @Test
    void approvesASpendWithinTheLimitAndDrawsDownAvailableBalance() throws Exception {
        long cardId = createCard(100_00L);

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 30_00L, "USD", "AWS"), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.declineReason").doesNotExist());

        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(30_00)))
                .andExpect(jsonPath("$.availableMinor", is(70_00)));
    }

    @Test
    void declinesOverTheLimitButStillRecordsTheAttempt() throws Exception {
        long cardId = createCard(50_00L);

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 80_00L, "USD", "Dell"), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DECLINED")))
                .andExpect(jsonPath("$.declineReason", is("LIMIT_EXCEEDED")));

        // A decline consumes no balance...
        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(0)));

        // ...but is still queryable history.
        mockMvc.perform(get("/api/transactions").param("cardId", String.valueOf(cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].status", is("DECLINED")));
    }

    @Test
    void declinesOnAFrozenCard() throws Exception {
        long cardId = createCard(100_00L);

        mockMvc.perform(patch("/api/cards/" + cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateCardRequest(Card.Status.FROZEN, 100_00L))));

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 10_00L, "USD", "AWS"), null))
                .andExpect(jsonPath("$.status", is("DECLINED")))
                .andExpect(jsonPath("$.declineReason", is("CARD_FROZEN")));
    }

    @Test
    void replayingAnIdempotencyKeyReturnsTheOriginalChargeInsteadOfDoubleCharging() throws Exception {
        long cardId = createCard(100_00L);
        AuthorizeRequest request = new AuthorizeRequest(cardId, 25_00L, "USD", "GitHub");

        String first = mockMvc.perform(authorize(request, "key-abc-123"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 200, not 201 — the caller can tell this was a replay, not a new authorization.
        mockMvc.perform(authorize(request, "key-abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(json.readTree(first).get("id").asInt())));

        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(25_00)));
    }

    /**
     * The limit is only real if it holds when two charges land at once. Without a row lock on the
     * card, both threads read the same approved total and both get APPROVED.
     */
    @Test
    void twoConcurrentChargesCannotBothConsumeTheSameRemainingLimit() throws Exception {
        long cardId = createCard(100_00L);          // room for exactly one of the two charges below
        int threads = 2;

        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<AuthorizeResult>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();
                return transactionService.authorize(
                        new AuthorizeRequest(cardId, 60_00L, "USD", "Concurrent"), null);
            }));
        }
        startTogether.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        long approved = 0;
        for (Future<AuthorizeResult> result : results) {
            if (result.get().transaction().status() == Transaction.Status.APPROVED) {
                approved++;
            }
        }

        assertThat(approved).as("exactly one 60.00 charge fits inside a 100.00 limit").isEqualTo(1);
        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(60_00)))
                .andExpect(jsonPath("$.availableMinor", is(40_00)));
    }

    /**
     * A key identifies one request, not "some request". Reusing it with different parameters is a
     * client bug; silently returning the original charge would hide it.
     */
    @Test
    void reusingAnIdempotencyKeyWithDifferentParametersIsRejected() throws Exception {
        long cardId = createCard(100_00L);

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 25_00L, "USD", "GitHub"), "key-reuse-1"))
                .andExpect(status().isCreated());

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 90_00L, "USD", "GitHub"), "key-reuse-1"))
                .andExpect(status().isConflict());

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 25_00L, "USD", "Figma"), "key-reuse-1"))
                .andExpect(status().isConflict());

        // The original charge stands, and nothing was charged twice.
        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(25_00)));
    }

    /**
     * `spent + amount > limit` overflows for a large enough amount and wraps negative, which reads
     * as "under the limit" and approves the charge. Needs prior spend to push the sum over the top.
     */
    @Test
    void anAmountLargeEnoughToOverflowIsDeclinedNotApproved() throws Exception {
        long cardId = createCard(100_00L);

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 60_00L, "USD", "AWS"), null))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, Long.MAX_VALUE, "USD", "Overflow"), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DECLINED")))
                .andExpect(jsonPath("$.declineReason", is("LIMIT_EXCEEDED")));

        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(jsonPath("$.spentMinor", is(60_00)));
    }

    @Test
    void spendingExactlyTheLimitIsApprovedAndOneMinorUnitMoreIsNot() throws Exception {
        long atLimit = createCard(100_00L);
        mockMvc.perform(authorize(new AuthorizeRequest(atLimit, 100_00L, "USD", "Exact"), null))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        long overLimit = createCard(100_00L);
        mockMvc.perform(authorize(new AuthorizeRequest(overLimit, 100_01L, "USD", "OneOver"), null))
                .andExpect(jsonPath("$.status", is("DECLINED")))
                .andExpect(jsonPath("$.declineReason", is("LIMIT_EXCEEDED")));
    }

    @Test
    void rejectsACurrencyThatDoesNotMatchTheCard() throws Exception {
        long cardId = createCard(100_00L);

        mockMvc.perform(authorize(new AuthorizeRequest(cardId, 10_00L, "EUR", "Figma"), null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForAnUnknownCard() throws Exception {
        mockMvc.perform(authorize(new AuthorizeRequest(999_999L, 10_00L, "USD", "AWS"), null))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder authorize(AuthorizeRequest request, String idempotencyKey)
            throws Exception {
        var builder = post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request));
        return idempotencyKey == null ? builder : builder.header("Idempotency-Key", idempotencyKey);
    }

    private long createCard(long limitMinor) throws Exception {
        String body = mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateCardRequest("Ada Lovelace", "4242", limitMinor, "USD"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asLong();
    }
}
