package com.jompastech.backend.exception;


/**
 * Exception for missing entity scenarios
 * Typlically results in HTTP 404 Not Found response
 */

public class EntityNotFoundException extends RuntimeException{
    public EntityNotFoundException(String message){
        super(message);
    }
}
