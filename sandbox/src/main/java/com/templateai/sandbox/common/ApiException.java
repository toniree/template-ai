package com.templateai.sandbox.common;

import org.springframework.http.HttpStatus;

/**
 * The only exception type this app throws deliberately. Carries the HTTP status with it so
 * {@link GlobalExceptionHandler} needs no per-type handler, and adding a new failure mode costs
 * a factory method rather than a new class.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    private ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** No usable caller identity — see {@link CurrentUser}. Not "you may not", which is 403. */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /** Identity is known but not allowed to touch this row. Distinct from 404 on purpose. */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    /** Well-formed request that violates a business rule (not a schema/validation failure). */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** State conflict: duplicate create, concurrent in-flight idempotent request, stale version. */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
