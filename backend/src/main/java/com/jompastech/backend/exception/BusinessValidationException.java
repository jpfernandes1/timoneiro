package com.jompastech.backend.exception;

/**
 * Exception for business rule violations
 * Typically results in HTTP 409 Conflict response
 */

public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
