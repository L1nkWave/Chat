package org.linkwave.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtAccessParser accessParser;
    private final DeactivatedTokenRepository tokenRepository;

    @GetMapping("/validate-token")
    public ResponseEntity<ApiError> validateToken(@NonNull HttpServletRequest request) {

        final var authHeader = request.getHeader(AUTHORIZATION);
        final var apiError = ApiError.builder()
                .path(request.getRequestURI())
                .status(BAD_REQUEST.value())
                .timestamp(Instant.now());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            final var token = accessParser.parse(authHeader.substring(7));
            if (token == null || tokenRepository.existsById(token.id())) {
                return ResponseEntity.status(UNAUTHORIZED)
                        .body(apiError.message("Invalid access token").build());
            }

            return noContent().build();
        }

        return badRequest().body(apiError.message("Bearer is not present").build());
    }

}
