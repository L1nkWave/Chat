package org.linkwave.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.dto.ApiError;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.springframework.http.HttpMethod.POST;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RequestMatcher registrationRequestMatcher = new AntPathRequestMatcher("/api/v1/users/register", POST.name());
    private final RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/v1/users/**");
    private final AuthenticationConverter authenticationConverter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (registrationRequestMatcher.matches(request) || !requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final Authentication authentication = authenticationConverter.convert(request);
        if (authentication == null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(SC_BAD_REQUEST);

            @Cleanup final var os = response.getOutputStream();
            objectMapper.writeValue(os, ApiError.builder()
                    .path(request.getRequestURI())
                    .timestamp(Instant.now())
                    .status(SC_BAD_REQUEST)
                    .message("Access token is invalid")
                    .build());
            os.flush();
            return;
        }

        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }

}