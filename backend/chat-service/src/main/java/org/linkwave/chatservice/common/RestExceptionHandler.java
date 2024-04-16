package org.linkwave.chatservice.common;

import jakarta.servlet.http.HttpServletRequest;
import org.linkwave.chatservice.api.ApiResponseClientErrorException;
import org.linkwave.chatservice.api.ServiceErrorException;
import org.linkwave.chatservice.chat.ChatMemberPermissionsDenied;
import org.linkwave.chatservice.chat.ChatNotFoundException;
import org.linkwave.chatservice.message.MessageNotFoundException;
import org.linkwave.shared.dto.ApiError;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class RestExceptionHandler {

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationErrors(@NonNull MethodArgumentNotValidException e) {
        return e.getBindingResult().getAllErrors()
                .stream()
                .map(error -> (FieldError) error)
                .filter(error -> nonNull(error.getDefaultMessage()))
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({
            BadCredentialsException.class,
            ApiResponseClientErrorException.class,
            UnacceptableRequestDataException.class,
            IllegalArgumentException.class,
            MultipartException.class,
            MissingServletRequestPartException.class
    })
    public ApiError handleBadRequest(@NonNull Exception e, @NonNull HttpServletRequest request) {
        e = revealCauseIfNeed(e);
        return ApiError.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(BAD_REQUEST.value())
                .build();
    }

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler({
            ChatNotFoundException.class,
            MessageNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ApiError handleNotFoundResourceError(@NonNull RuntimeException e, @NonNull HttpServletRequest request) {
        return ApiError.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(NOT_FOUND.value())
                .build();
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler({
            PrivacyViolationException.class,
            ChatOptionsViolationException.class,
            ChatMemberPermissionsDenied.class
    })
    public ApiError handleForbiddenAccess(@NonNull RuntimeException e, @NonNull HttpServletRequest request) {
        return ApiError.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(FORBIDDEN.value())
                .build();
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ServiceErrorException.class)
    public ApiError handleServerError(@NonNull Exception e, @NonNull HttpServletRequest request) {
        e = revealCauseIfNeed(e);
        return ApiError.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(INTERNAL_SERVER_ERROR.value())
                .build();
    }

    private Exception revealCauseIfNeed(Exception e) {
        return e instanceof UndeclaredThrowableException
                ? (RuntimeException) e.getCause()
                : e;
    }

}
