package com.example.savingswallet.infrastructure.rest.error;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error response body returned by the REST API.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param message   human-readable error message
 * @param path      request URI that failed
 * @param fieldErrors per-field validation errors (empty when not applicable)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}