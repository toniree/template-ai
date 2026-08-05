package com.templateai.sandbox.common;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
