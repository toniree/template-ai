package com.templateai.sandbox.support;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;

import com.templateai.sandbox.common.ApiError;

/**
 * One-line assertions for the outcomes every API has: created, rejected, missing, conflicting.
 *
 * <pre>{@code
 * import static com.templateai.sandbox.support.ApiErrors.*;
 *
 * http.perform(postJson("/api/tasks", request)).andExpect(created());
 * http.perform(postJson("/api/tasks", blank)).andExpect(validationError("title must not be blank"));
 * http.perform(get("/api/tasks/999999")).andExpect(notFound());
 * http.perform(postJson("/api/seats/1/book", request)).andExpect(conflict());
 * }</pre>
 *
 * <p>These check the <b>whole {@link ApiError} shape</b>, not just the status line — the status
 * field inside the body, the message, and the per-field details. That matters because the frontend
 * parses exactly one error shape: a handler that returns the right status code with a differently
 * shaped body still breaks the UI, and a bare {@code status().isBadRequest()} would not notice.
 *
 * <p>Use these for the error paths and plain MockMvc matchers for asserting the contents of a
 * successful response — there is nothing to centralise about a domain-specific success body.
 */
public final class ApiErrors {

    private ApiErrors() {
    }

    /** 201 with a usable {@code Location} — the two halves of a correct create. */
    public static ResultMatcher created() {
        return ResultMatcher.matchAll(
                status().isCreated(),
                header().string("Location", notNullValue()));
    }

    /**
     * 400 from Bean Validation, with one detail line per rejected field.
     *
     * @param expectedDetails detail strings that must be present, e.g. {@code "title must not be
     *                        blank"}. Pass none to assert only the shape.
     */
    public static ResultMatcher validationError(String... expectedDetails) {
        ResultMatcher shape = ResultMatcher.matchAll(
                apiError(HttpStatus.BAD_REQUEST),
                jsonPath("$.message", is("Validation failed")));

        return expectedDetails.length == 0
                ? shape
                : ResultMatcher.matchAll(shape, jsonPath("$.details", hasItems(expectedDetails)));
    }

    /** 404 — the id does not exist. */
    public static ResultMatcher notFound() {
        return apiError(HttpStatus.NOT_FOUND);
    }

    /**
     * 409 — the request conflicts with current state. The status your concurrency invariant returns
     * to whoever lost, so this is usually the assertion on the losing side of a contended write.
     */
    public static ResultMatcher conflict() {
        return apiError(HttpStatus.CONFLICT);
    }

    /** 400 for a well-formed request that breaks a business rule (not a schema failure). */
    public static ResultMatcher badRequest() {
        return apiError(HttpStatus.BAD_REQUEST);
    }

    /** 401 — no usable caller identity. What {@code CurrentUser.require()} produces. */
    public static ResultMatcher unauthorized() {
        return apiError(HttpStatus.UNAUTHORIZED);
    }

    /** 403 — identity known, not allowed to touch this row. */
    public static ResultMatcher forbidden() {
        return apiError(HttpStatus.FORBIDDEN);
    }

    /** 402 — well-formed, authorized request declined for lack of funds. */
    public static ResultMatcher paymentRequired() {
        return apiError(HttpStatus.PAYMENT_REQUIRED);
    }

    /** The generic form: correct status line, and a body that is a well-formed {@link ApiError}. */
    public static ResultMatcher apiError(HttpStatus expected) {
        return ResultMatcher.matchAll(
                status().is(expected.value()),
                jsonPath("$.status", is(expected.value())),
                jsonPath("$.error", is(expected.getReasonPhrase())),
                jsonPath("$.timestamp", notNullValue()),
                jsonPath("$.message", notNullValue()));
    }
}
