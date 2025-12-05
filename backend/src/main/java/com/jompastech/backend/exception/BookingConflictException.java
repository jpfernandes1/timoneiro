package com.jompastech.backend.exception;

/**
 * Exception thrown when a booking request conflicts with existing reservations
 * such as overlapping time periods for the same boat.
 *
 * This exception indicates a temporal conflict where the requested booking period
 * overlaps with already confirmed bookings, preventing double-booking scenarios.
 */
public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}