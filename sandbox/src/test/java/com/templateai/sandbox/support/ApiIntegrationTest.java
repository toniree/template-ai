package com.templateai.sandbox.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.templateai.sandbox.common.CurrentUser;

/**
 * Base for API tests: real HTTP, real database, no mocks. Extend it and you get {@link #http} and
 * {@link #json} wired, on the isolated {@code test} profile.
 *
 * <p>Everything here composes with ordinary MockMvc rather than hiding it — build requests with the
 * usual {@code get(...)}/{@code post(...)} static imports and reach for these helpers only where
 * they save a line. A test you can read aloud beats a test that needs this class explained first.
 *
 * <p><b>Deliberately not {@code @Transactional}.</b> Rolling each test back would be tidier, but it
 * also hides commit-time behaviour — constraint violations that only fire on flush, and anything
 * concurrent (see {@link Concurrently}). The suite shares one H2 database instead, so each test
 * creates the rows it asserts on and never asserts on table-wide counts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    @Autowired
    protected MockMvc http;

    @Autowired
    protected ObjectMapper json;

    /**
     * Calls as this user, by setting the header {@link CurrentUser} reads.
     *
     * <pre>{@code http.perform(get("/api/orders/mine").with(as(userId)))}</pre>
     *
     * <p>It is a stand-in for an authenticated principal, so tests can also assert what happens
     * when it is <em>absent</em> — call without {@code as(...)} and expect 401.
     */
    protected static RequestPostProcessor as(long userId) {
        return request -> {
            request.addHeader(CurrentUser.HEADER, Long.toString(userId));
            return request;
        };
    }

    /** JSON POST. Pass a request record; it is serialised with the app's own ObjectMapper. */
    protected MockHttpServletRequestBuilder postJson(String path, Object body) {
        return withJson(MockMvcRequestBuilders.post(path), body);
    }

    /** JSON PATCH — the partial-update case, where body is usually a sparse record or a raw string. */
    protected MockHttpServletRequestBuilder patchJson(String path, Object body) {
        return withJson(MockMvcRequestBuilders.patch(path), body);
    }

    /** JSON PUT. */
    protected MockHttpServletRequestBuilder putJson(String path, Object body) {
        return withJson(MockMvcRequestBuilders.put(path), body);
    }

    /**
     * Serialises to JSON. A {@link String} is passed through untouched, so a test can send a
     * deliberately malformed or partial body that no record could represent.
     */
    protected String asJson(Object body) {
        if (body instanceof String raw) {
            return raw;
        }
        try {
            return json.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialise test body: " + body, e);
        }
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder builder, Object body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(asJson(body));
    }
}
