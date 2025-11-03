package com.jompastech.backend.exception;

/**
 * Exception thrown when booking validation fails against business rules
 * such as invalid dates, pricing issues, or availability constraints.
 *
 * Represents violations of domain rules that prevent a booking from being
 * created, distinct from system-level errors or data conflicts.
 */
public class BookingValidationException extends RuntimeException {
    public BookingValidationException(String message) {
        super(message);
    }

    public BookingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}