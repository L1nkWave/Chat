package org.linkwave.chatservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthConfigurer extends AbstractHttpConfigurer<JwtAuthConfigurer, HttpSecurity> {

    private AuthenticationConverter authenticationConverter;
    private ObjectMapper objectMapper;

    @Override
    public void configure(@NonNull HttpSecurity builder) {
        log.debug("-> configure(): build JwtAuthentication filter");
        final var authenticationFilter = new JwtAuthenticationFilter(authenticationConverter, objectMapper);
        builder.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    public JwtAuthConfigurer setAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        this.authenticationConverter = authenticationConverter;
        return this;
    }

    public JwtAuthConfigurer setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

}
