package org.linkwave.userservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final int TOKEN_START_POSITION = 7;

    private final UserDetailsService userDetailsService;
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.info("-> doFilterInternal()");
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String jwt = null;

        if (header != null) {
            jwt = header.substring(TOKEN_START_POSITION);
        }

        if (jwt != null && !jwt.isBlank()) {

            log.info("-> doFilterInternal(): jwt is present");

            Optional<DecodedJWT> optDecodedJWT = jwtProvider.decode(jwt, Token.ACCESS);
            if (optDecodedJWT.isPresent()) {
                log.info("-> doFilterInternal(): jwt is valid");
                DecodedJWT decodedJWT = optDecodedJWT.get();

                UserDetails userDetails = userDetailsService.loadUserByUsername(
                        decodedJWT.getClaim("username").asString()
                );

                Authentication authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.error("-> doFilterInternal(): invalid jwt");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                fillResponseBody(response, "invalid jwt");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @SneakyThrows
    private void fillResponseBody(HttpServletResponse response, String msgError) {
        @Cleanup var oos = new ObjectOutputStream(response.getOutputStream());
        response.setContentLength(msgError.length());
        response.setContentType("text/plain");
        oos.writeUTF(msgError);
    }

}