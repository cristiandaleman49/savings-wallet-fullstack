package com.example.savingswallet.infrastructure.rest.error;

import com.example.savingswallet.application.usecase.SavingsGoalNotFoundException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into the uniform {@link ApiError} response body.
 *
 * <p>Only HTTP/request concerns live here: malformed shapes map to 400, the
 * goal-not-found (including ownership mismatch) maps to 404, domain business
 * rule violations map to 422, and anything unexpected maps to 500.
 *
 * <p>SSE exception: when the async dispatch of {@code GET
 * /api/v1/savings-goals/events} fails after the response was committed as
 * {@code text/event-stream} (client disconnected mid-stream), no
 * {@code ApiError} body is rendered. There is no message converter capable of
 * writing {@code ApiError} with that content type, so attempting it only
 * produces {@code HttpMessageNotWritableException}. Returning {@code null}
 * tells Spring the error was handled with no body; the dead emitter is
 * already removed by the SSE lifecycle callbacks. Synchronous errors on the
 * {@code /events} endpoint itself (e.g. missing {@code userId} parameter,
 * dispatched as {@code REQUEST} before the stream starts) still render the
 * normal {@code ApiError} JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiError> handleUnreadable(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MissingServletRequestParameterException missing
                ? "Missing required parameter '" + missing.getParameterName() + "'"
                : "Malformed request body";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", request, List.of());
    }

    @ExceptionHandler(InvalidCurrencyCodeException.class)
    public ResponseEntity<ApiError> handleInvalidCurrency(InvalidCurrencyCodeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(SavingsGoalNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(SavingsGoalNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleDomainViolation(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request,
                                                    HttpServletResponse response) {
        if (isCommittedSseDispatch(request, response)) {
            // SSE lifecycle failure on an already-committed text/event-stream
            // response (dead client). Handle with no body instead of trying to
            // serialize ApiError, which has no capable converter for the SSE
            // content type and would only raise HttpMessageNotWritableException.
            log.debug("SSE dispatch failed on committed stream, skipping ApiError body", ex);
            return null;
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, List.of());
    }

    /**
     * True only for async dispatches of the SSE stream endpoint after the
     * response headers (content type {@code text/event-stream}) were committed.
     * Every other dispatch — including synchronous errors on the very same
     * endpoint before the stream starts — renders the normal {@code ApiError}.
     */
    private static boolean isCommittedSseDispatch(HttpServletRequest request, HttpServletResponse response) {
        String contentType = response.getContentType();
        return request.getDispatcherType() == DispatcherType.ASYNC
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/events")
                && (response.isCommitted()
                        || (contentType != null && contentType.startsWith("text/event-stream")));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           List<ApiError.FieldError> fieldErrors) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}