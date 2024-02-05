package org.linkwave.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.linkwave.auth.dto.ApiError;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@RequiredArgsConstructor
public class UnAuthorizedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {

        int status;

        if (authException instanceof BadCredentialsException) {
            status = SC_BAD_REQUEST;
        } else {
            status = SC_UNAUTHORIZED;
        }

        final var apiError = ApiError.builder()
                .path(request.getRequestURI())
                .status(status)
                .message(authException.getMessage())
                .timestamp(Instant.now())
                .build();

        @Cleanup final var outputStream = response.getOutputStream();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(outputStream, apiError);
        outputStream.flush();
    }

}
