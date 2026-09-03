package com.fcproject.infrastructure.exceptions;

import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.core.exceptions.InvalidCredentialsException;
import com.fcproject.application.core.exceptions.LoginBlockedException;
import com.fcproject.application.core.exceptions.InvalidValueException;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import com.fcproject.application.core.exceptions.BusinessRuleException;
import com.fcproject.application.core.exceptions.RequiredFieldException;
import com.fcproject.application.core.exceptions.ResourceNotFoundException;
import com.fcproject.application.core.exceptions.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalHandler.class);

    @ExceptionHandler({RequiredFieldException.class, InvalidValueException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), "INVALID_REQUEST");
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), exception.getCode());
    }

    @ExceptionHandler(BusinessConflictException.class)
    ResponseEntity<ProblemDetail> handleBusinessConflict(BusinessConflictException exception) {
        return response(HttpStatus.CONFLICT, "Resource conflict", exception.getMessage(), exception.getCode());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return response(
                HttpStatus.CONFLICT,
                "Concurrent modification",
                "The resource was changed by another request",
                "CONCURRENT_MODIFICATION"
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, "Resource conflict", exception.getMessage(), "USER_ALREADY_EXISTS");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception) {
        LOGGER.warn("Database constraint violation", exception);
        return response(
                HttpStatus.CONFLICT,
                "Resource conflict",
                "The operation violates a data constraint",
                "DATA_CONFLICT"
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler({AuthenticationException.class, InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    ResponseEntity<ProblemDetail> handleUnauthorized(RuntimeException exception) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid credentials or token",
                "INVALID_CREDENTIALS"
        );
    }

    @ExceptionHandler(LoginBlockedException.class)
    ResponseEntity<ProblemDetail> handleLoginBlocked(LoginBlockedException exception) {
        ProblemDetail problem = problem(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many login attempts",
                "Login is temporarily blocked; retry later",
                "LOGIN_TEMPORARILY_BLOCKED"
        );
        problem.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected request failure", exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred",
                "INTERNAL_ERROR"
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more fields are invalid",
                "VALIDATION_ERROR"
        );
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String title,
            String detail,
            String code
    ) {
        return ResponseEntity.status(status).body(problem(status, title, detail, code));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://cointrol.dev/problems/" + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        return problem;
    }
}
