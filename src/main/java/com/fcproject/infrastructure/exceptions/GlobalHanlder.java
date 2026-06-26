package com.fcproject.infrastructure.exceptions;


import com.fcproject.infrastructure.exceptions.global.DataNotFound;
import com.fcproject.infrastructure.exceptions.global.InvalidValueException;
import com.fcproject.infrastructure.exceptions.global.NotAllFieldsFilledException;
import com.fcproject.infrastructure.exceptions.user.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@ControllerAdvice
@RestController
public class GlobalHanlder extends ResponseEntityExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalHanlder.class.getName());

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public final ResponseEntity<ExceptionResponse> handleGlobalErrorException(Exception ex, WebRequest request) {
        ExceptionResponse exResponse = new ExceptionResponse(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        logger.error("Internal Server Error {}", ex.getMessage());
        return new ResponseEntity<>(exResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({
            NotAllFieldsFilledException.class,
            InvalidValueException.class,})
    public final ResponseEntity<ExceptionResponse> handleInvalidField(Exception ex, WebRequest request) {
        ExceptionResponse exResponse = new ExceptionResponse(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(exResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public final ResponseEntity<ExceptionResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpStatus status, WebRequest request) {
        ExceptionResponse exResponse = new ExceptionResponse(
                new Date(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(exResponse, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(DataNotFound.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFound(DataNotFound ex, HttpStatus status, WebRequest request) {

        return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    }
}