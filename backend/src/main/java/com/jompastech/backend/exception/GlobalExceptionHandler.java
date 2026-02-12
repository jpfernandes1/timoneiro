package com.jompastech.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Timoneiro application.
 *
 * Provides consistent and structured error responses across all REST endpoints.
 * Uses a standardized error response format to improve client-side error handling
 * and debugging capabilities.
 *
 * Design Decision: Using a record for ErrorResponse ensures immutability and
 * clear data structure while reducing boilerplate code.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Standardized error response structure for all API errors.
     *
     * Includes timestamp for debugging, HTTP status, error type,
     * human-readable message, and request path for correlation.
     */
    record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    /**
     * Handles UserNotFoundException when requested user resources are not found.
     *
     * Design Decision: Returning 404 Not Found with specific error message
     * rather than generic error to improve client-side error handling.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            UserNotFoundException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles EmailAlreadyInUseException for user registration conflicts.
     *
     * Design Decision: Using 409 Conflict status code as recommended by
     * HTTP specification for resource creation conflicts.
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            EmailAlreadyInUseException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                409,
                "Conflict",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handles IllegalArgumentException for business rule violations.
     *
     * Added to support MessageService validation errors and other
     * business logic violations. Returns 400 Bad Request as these
     * are client-side input or logic errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                400,
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles MethodArgumentNotValidException for request body validation errors.
     *
     * Design Decision: Using Map instead of ErrorResponse to provide
     * field-level error details for form validation scenarios.
     * This format is more usable for client-side form validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles ConstraintViolationException for query parameter and path variable validation.
     *
     * Design Decision: Separate from MethodArgumentNotValidException because
     * ConstraintViolationException handles different validation contexts
     * (query params, path variables vs request body).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolations(
            ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Fallback handler for all uncaught exceptions.
     *
     * Design Decision: Returning generic error message to avoid exposing
     * internal implementation details while logging the actual exception
     * for internal debugging purposes.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                500,
                "Internal Server Error",
                "An unexpected error occurred",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Handles PaymentProcessingException
     * It throws this error when occur some error with payment on PaymentService
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessingException(
            PaymentProcessingException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                402,
                "Payment Required",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                403,
                "Forbidden",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(BookingCreationException.class)
    public ResponseEntity<ErrorResponse> handleBookingCreationException(
            BookingCreationException ex, HttpServletRequest req) {
        var body = new ErrorResponse(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        if (ex.getMessage().contains("Boat not found") || ex.getMessage().contains("User not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        if (ex.getMessage().contains("availability window")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body); // 409
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        log.error("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason(), ex);

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason(),
                request.getRequestURI()
                );

        return new ResponseEntity<>(error, ex.getStatusCode());
    }
}