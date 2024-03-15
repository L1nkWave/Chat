package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.dto.ApiError;
import org.linkwave.userservice.exception.LimitExceededException;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

import static java.time.Instant.now;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class RestControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        log.debug("-> handleMethodArgumentNotValid(...)");

        Map<String, String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> (FieldError) error)
                .collect(Collectors.toMap(FieldError::getField, fieldError -> {
                    String message = fieldError.getDefaultMessage();
                    return message == null ? "" : message;
                }));

        return new ResponseEntity<>(errors, BAD_REQUEST);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            LimitExceededException.class,
            UnacceptableRequestDataException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleBadRequestError(RuntimeException ex, HttpServletRequest request) {
        log.debug("-> handleRuntimeExceptions(...): path={}, msg={}", request.getRequestURI(), ex.getMessage());
        return new ApiError(request.getRequestURI(), ex.getMessage(), BAD_REQUEST.value(), now());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ApiError handleRuntimeError(RuntimeException ex, HttpServletRequest request) {
        log.debug("-> handleRuntimeExceptions(...): path={}, msg={}", request.getRequestURI(), ex.getMessage());
        return new ApiError(request.getRequestURI(), ex.getMessage(), INTERNAL_SERVER_ERROR.value(), now());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ApiError handleNotFoundResource(RuntimeException ex, HttpServletRequest request) {
        return new ApiError(request.getRequestURI(), ex.getMessage(), NOT_FOUND.value(), now());
    }

}