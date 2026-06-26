package com.fcproject.infrastructure.exceptions.global;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NO_CONTENT)
public class DataNotFound extends RuntimeException {
    public DataNotFound(String message) {
        super(message);
    }
}
