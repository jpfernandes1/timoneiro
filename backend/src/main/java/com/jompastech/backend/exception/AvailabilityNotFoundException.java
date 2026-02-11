package com.jompastech.backend.exception;

public class AvailabilityNotFoundException extends RuntimeException {

    public AvailabilityNotFoundException(String message) {
        super(message);
    }

    public AvailabilityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}