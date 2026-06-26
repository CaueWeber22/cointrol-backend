package com.fcproject.infrastructure.exceptions;


import java.util.Date;

public record ExceptionResponse(Date timestamp, String message, String details) {
}
