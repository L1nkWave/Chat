package org.linkwave.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trả lỗi nghiệp vụ dạng JSON kèm thông báo, và ghi log. Không có handler này,
 * lỗi bị chuyển sang /error không có session và trả về 403 rỗng.
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handle(ResponseStatusException e, HttpServletRequest request) {
        final int status = e.getStatusCode().value();
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status, e.getReason());

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("path", request.getRequestURI());
        body.put("status", status);
        body.put("message", e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

}