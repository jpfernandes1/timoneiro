package com.jompastech.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Returns HTTP 409 Conflict
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("Email already registered: " + email);
    }

    public EmailAlreadyInUseException(String email, Throwable cause) {
        super("Email already registered: " + email, cause);
    }
}