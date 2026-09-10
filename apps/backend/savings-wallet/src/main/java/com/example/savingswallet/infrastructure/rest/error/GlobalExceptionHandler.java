package com.example.savingswallet.infrastructure.rest.error;

import com.example.savingswallet.application.usecase.SavingsGoalNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into the uniform {@link ApiError} response body.
 *
 * <p>Only HTTP/request concerns live here: malformed shapes map to 400, the
 * goal-not-found (including ownership mismatch) maps to 404, domain business
 * rule violations map to 422, and anything unexpected maps to 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, List.of());
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
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           List<ApiError.FieldError> fieldErrors) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}