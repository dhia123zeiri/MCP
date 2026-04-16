package com.doctor_office.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PetAlreadyExistsException extends RuntimeException {

    public PetAlreadyExistsException(String message) {
        super(message);
    }
}