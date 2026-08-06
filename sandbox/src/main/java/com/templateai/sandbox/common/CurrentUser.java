package com.templateai.sandbox.common;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Who is calling. Reads the {@code X-User-Id} request header.
 *
 * <p><b>This is not authentication.</b> The caller supplies the header, so any caller can claim to
 * be any user. It is a development stand-in for an authenticated principal, and the reason it
 * exists as a single class is that swapping it for the real thing — a verified JWT claim, a session
 * lookup, a {@code SecurityContextHolder} read — is then a change to {@link #find()} alone, with no
 * service or controller signature touched. Say exactly that when asked about it; do not describe
 * the app as having auth.
 *
 * <p>Inject it into a service and call {@link #require()} rather than threading a user id down
 * through controller parameters. A controller that needs the id for a {@code Location} header or a
 * log line can inject this too.
 *
 * <pre>{@code
 * public OrderResponse place(PlaceOrderRequest request) {
 *     long userId = currentUser.require();
 *     ...
 * }
 * }</pre>
 */
@Component
public class CurrentUser {

    public static final String HEADER = "X-User-Id";

    /**
     * The calling user's id, or 401 when the header is absent, blank, or not a number.
     *
     * <p>Returns a primitive: there is no "maybe" here by design. An endpoint that genuinely allows
     * anonymous callers should use {@link #find()} and decide for itself.
     */
    public long require() {
        Long id = find();
        if (id == null) {
            throw ApiException.unauthorized("Missing or invalid " + HEADER + " header");
        }
        return id;
    }

    /** The calling user's id, or {@code null} outside a request or when the header is unusable. */
    public Long find() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) {
            return null;                       // async worker, scheduled task, plain unit test
        }
        String raw = servlet.getRequest().getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;                       // a malformed header is "no identity", not a 500
        }
    }
}
