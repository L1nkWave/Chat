package org.linkwave.chat.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

    private final UserDetailsService userDetailsService;
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.info("-> doFilterInternal()");

        String jwt = request.getHeader("Authorization");
        if (jwt != null) {
            Optional<DecodedJWT> optDecodedJWT = jwtProvider.decode(jwt, Token.ACCESS);
            if (optDecodedJWT.isPresent()) {
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
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                fillResponseBody(response, "invalid jwt");
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