package com.jompastech.backend.exception;

/**
 * Exception thrown when booking creation fails due to data integrity issues
 * or missing required entities.
 *
 * Typically occurs when referenced entities (user, boat) don't exist or
 * when fundamental creation prerequisites are not met.
 */
public class BookingCreationException extends RuntimeException {
    public BookingCreationException(String message) {
        super(message);
    }

    public BookingCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}