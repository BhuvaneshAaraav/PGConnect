package com.pgconnect.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) details.putIfAbsent(error.getField(), error.getDefaultMessage());
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Validation failed", "Please check the submitted fields.", request.getRequestURI(), details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        String reason = exception.getReason() == null ? "Request could not be completed" : exception.getReason();
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status, "Request failed", reason, request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> database(DataAccessException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(Instant.now(), 503, "Database unavailable", "The database request could not be completed. Please try again.", request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(Instant.now(), 401, "Authentication failed", "Invalid email or password.", request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Invalid request", "The request body or query value is invalid.", request.getRequestURI(), Map.of()));
    }

    public record ApiError(Instant timestamp, int status, String error, String message, String path, Map<String, String> details) {}
}
