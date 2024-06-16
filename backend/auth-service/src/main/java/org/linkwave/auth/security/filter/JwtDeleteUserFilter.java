package org.linkwave.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.linkwave.auth.dto.UserDeleteRequest;
import org.linkwave.auth.security.UnAuthorizedAuthenticationEntryPoint;
import org.linkwave.auth.service.UserService;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.Token;
import org.linkwave.shared.utils.Bearers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtDeleteUserFilter extends OncePerRequestFilter {

    private final RequestMatcher matcher = new AntPathRequestMatcher("/api/v1/users", HttpMethod.DELETE.name());

    private final ObjectMapper objectMapper;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessParser jwtAccessParser;
    private final UserService userService;

    public JwtDeleteUserFilter(ObjectMapper objectMapper, JwtAccessParser jwtAccessParser, UserService userService) {
        this.objectMapper = objectMapper;
        this.jwtAccessParser = jwtAccessParser;
        this.userService = userService;
        this.authenticationEntryPoint = new UnAuthorizedAuthenticationEntryPoint(objectMapper);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!matcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            final String password;
            try {
                final UserDeleteRequest deleteRequest = objectMapper.readValue(request.getInputStream(), UserDeleteRequest.class);
                password = deleteRequest.password();
            } catch (IOException e) {
                throw new BadCredentialsException("Invalid request body");
            }

            if (header == null || !header.startsWith(Bearers.BEARER_PREFIX)) {
                throw new BadCredentialsException("Bearer is not present");
            }

            final Token accessToken = jwtAccessParser.parse(header.substring(Bearers.TOKEN_START_POSITION));
            userService.deleteAccount(accessToken, password);

            response.setContentLength(0);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

}
