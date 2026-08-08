package com.templateai.sandbox.common;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error the API can return is shaped here. Controllers and services never try/catch for
 * HTTP purposes — they throw {@link ApiException} and this turns it into a response.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} on purpose. That base class already maps the
 * whole Spring MVC exception family to correct status codes — unparseable path variables and query
 * params to 400, unknown routes to 404, wrong verb to 405. Without it, the catch-all at the bottom
 * swallows all of them and reports client mistakes as 500, which tells callers to retry something
 * that will never succeed. We only override where we want our own body or message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getStatus(), ex.getMessage(), List.of()));
    }

    /**
     * A unique or foreign-key constraint rejected the write — a duplicate, or a reference to a row
     * that isn't there. 409 says "your request conflicts with existing state", which is the caller's
     * problem to resolve; a 500 would tell them to retry something that can never succeed.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(DataIntegrityViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Request conflicts with existing data", List.of()));
    }

    /**
     * An optimistic-lock conflict: someone else updated the row between this transaction's read and
     * its write, and {@code @Version} caught it. That's a 409 for the same reason a constraint
     * violation is — the caller's view was stale, and re-reading and retrying is the fix.
     *
     * <p>Nothing in the sample domain declares {@code @Version}, so nothing throws this yet. It is
     * here so that adding {@code @Version} to an entity gives you the documented 409 immediately,
     * rather than a 500 from the catch-all below — which would tell the caller "our fault" about a
     * conflict that is retryable. Spring translates JPA's {@code OptimisticLockException} into this
     * type for repository operations, so handling the Spring exception covers both.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Resource was modified by another request; re-read and retry",
                        List.of()));
    }

    /**
     * PostgreSQL refused to commit because of contention, not because the request was wrong:
     * a serialization failure under {@code SERIALIZABLE} (SQLSTATE 40001), a deadlock the server
     * chose this transaction to break (40P01), or a lock-timeout waiting on {@code for update}.
     * Spring translates all three into subtypes of this exception.
     *
     * <p>Every one of them is retryable — the same request, sent again, can succeed — so a 500 is
     * the wrong answer: it tells the caller "our fault, state unknown" about a transaction that
     * cleanly rolled back. This handler only exists on the PostgreSQL branch; H2 does not produce
     * these, which is precisely the point of running the suite against the real database.
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleContention(PessimisticLockingFailureException ex) {
        log.warn("Transaction lost to contention: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Conflicting concurrent request; retry", List.of()));
    }

    /** Genuinely unexpected. Log the stack, never leak it to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of()));
    }

    /** Bean Validation failure on a @Valid @RequestBody — one detail line per bad field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .sorted()
                .toList();
        return ResponseEntity.status(status).body(body(HttpStatus.BAD_REQUEST, "Validation failed", details));
    }

    /** Single funnel for everything the base class handles, so the body shape stays uniform. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object ignoredBody,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus resolved = HttpStatus.valueOf(status.value());
        log.debug("{} for {}: {}", resolved, request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(status).headers(headers).body(body(resolved, message(ex, resolved), List.of()));
    }

    /** Useful enough to help the caller fix the request, never enough to leak internals. */
    private String message(Exception ex, HttpStatus status) {
        return switch (ex) {
            case MethodArgumentTypeMismatchException mismatch ->
                    "Invalid value for '%s'".formatted(mismatch.getName());
            case MissingServletRequestParameterException missing ->
                    "Missing required parameter '%s'".formatted(missing.getParameterName());
            case HttpMessageNotReadableException ignored -> "Malformed request body";
            default -> status.getReasonPhrase();
        };
    }

    private String describe(FieldError error) {
        return "%s %s".formatted(error.getField(), error.getDefaultMessage());
    }

    private ApiError body(HttpStatus status, String message, List<String> details) {
        return new ApiError(Instant.now(clock), status.value(), status.getReasonPhrase(), message, details);
    }
}
