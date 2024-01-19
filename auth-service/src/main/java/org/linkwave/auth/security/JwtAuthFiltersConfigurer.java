package org.linkwave.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.security.filter.JwtLogoutFilter;
import org.linkwave.auth.security.filter.JwtTokensInitializerFilter;
import org.linkwave.auth.security.filter.JwtTokensRefreshFilter;
import org.linkwave.auth.security.jwt.JwtAccessParser;
import org.linkwave.auth.security.jwt.JwtAccessSerializer;
import org.linkwave.auth.security.jwt.JwtRefreshParser;
import org.linkwave.auth.security.jwt.JwtRefreshSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFiltersConfigurer extends AbstractHttpConfigurer<JwtAuthFiltersConfigurer, HttpSecurity> {

    private final ObjectMapper objectMapper;
    private final DeactivatedTokenRepository tokenRepository;
    private final UserDetailsService userDetailsService;

    private AuthenticationManager authenticationManager;
    private JwtRefreshSerializer jwtRefreshSerializer;
    private JwtAccessSerializer jwtAccessSerializer;
    private JwtRefreshParser jwtRefreshParser;
    private JwtAccessParser jwtAccessParser;

    @Override
    public void configure(@NonNull HttpSecurity builder) {

        log.info("-> configure(): build filters");

        // setup filters
        final var jwtTokensInitializerFilter = new JwtTokensInitializerFilter(
                objectMapper, authenticationManager,
                jwtRefreshSerializer, jwtAccessSerializer
        );

        final var jwtTokensRefreshFilter = new JwtTokensRefreshFilter(
                jwtRefreshSerializer, jwtAccessSerializer, jwtRefreshParser,
                tokenRepository, objectMapper, userDetailsService
        );

        final var jwtLogoutFilter = new JwtLogoutFilter(objectMapper, jwtAccessParser, tokenRepository);

        // add filters to filter chain
        builder.addFilterAfter(jwtTokensInitializerFilter, ExceptionTranslationFilter.class);
        builder.addFilterAfter(jwtTokensRefreshFilter, ExceptionTranslationFilter.class);
        builder.addFilterAfter(jwtLogoutFilter, ExceptionTranslationFilter.class);
    }

    /*
        Injection AuthenticationProvider in this way because when we get it
        as shared object from HttpSecurity it creates a child AuthenticationProvider object attached to parent one.
        As a result the request will be authenticated twice (when credentials are invalid),
        first by child object then by parent.
     */
    public JwtAuthFiltersConfigurer setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        return this;
    }

    public JwtAuthFiltersConfigurer setJwtRefreshSerializer(JwtRefreshSerializer jwtRefreshSerializer) {
        this.jwtRefreshSerializer = jwtRefreshSerializer;
        return this;
    }

    public JwtAuthFiltersConfigurer setJwtAccessSerializer(JwtAccessSerializer jwtAccessSerializer) {
        this.jwtAccessSerializer = jwtAccessSerializer;
        return this;
    }

    public JwtAuthFiltersConfigurer setJwtRefreshParser(JwtRefreshParser jwtRefreshParser) {
        this.jwtRefreshParser = jwtRefreshParser;
        return this;
    }

    public JwtAuthFiltersConfigurer setJwtAccessParser(JwtAccessParser jwtAccessParser) {
        this.jwtAccessParser = jwtAccessParser;
        return this;
    }
}
