package org.linkwave.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.linkwave.auth.dto.UserLoginRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.io.IOException;

@RequiredArgsConstructor
public class CredentialsAuthenticationConverter implements AuthenticationConverter {

    private final ObjectMapper objectMapper;

    @Override
    public Authentication convert(@NonNull HttpServletRequest request) {
        try {
            final var loginRequest = objectMapper.readValue(request.getInputStream(), UserLoginRequest.class);
            return UsernamePasswordAuthenticationToken.unauthenticated(
                    loginRequest.username(),
                    loginRequest.password()
            );
        } catch (IOException e) {
            return null;
        }
    }

}
