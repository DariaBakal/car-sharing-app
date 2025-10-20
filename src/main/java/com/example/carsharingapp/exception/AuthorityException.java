package com.example.carsharingapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AuthorityException extends RuntimeException {
    public AuthorityException(String message) {
        super(message);
    }
}
