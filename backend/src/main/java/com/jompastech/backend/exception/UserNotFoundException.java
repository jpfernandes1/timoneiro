package com.jompastech.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    // For Id
    public UserNotFoundException(Long id) {
        super("User not found with ID: " + id);
    }

    // For email
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }

    // Generic
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}