package com.jompastech.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Returns HTTP 409 Conflict
public class CpfAlreadyInUseException extends RuntimeException {

    public CpfAlreadyInUseException(String cpf) {
        super("CPF already registered: " + cpf);
    }

    public CpfAlreadyInUseException(String cpf, Throwable cause) {
        super("CPF already registered: " + cpf, cause);
    }
}