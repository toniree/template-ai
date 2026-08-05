package com.templateai.sandbox.common;

import java.time.Instant;
import java.util.List;

/**
 * Single error shape for every non-2xx response, so the frontend has exactly one thing to parse.
 * {@code details} carries per-field validation messages; it is empty for everything else.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
}
