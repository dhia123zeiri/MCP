package com.doctor_office.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Used by REST controllers — keeps the technical detail for API consumers
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with the given input data %s : '%s'",
                resourceName, fieldName, fieldValue));
    }

    // Used by MCP tools — allows a clean custom message
    public ResourceNotFoundException(String message) {
        super(message);
    }
}