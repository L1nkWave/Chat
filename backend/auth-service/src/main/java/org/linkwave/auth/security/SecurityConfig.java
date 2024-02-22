package org.linkwave.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.JwtAccessSerializer;
import org.linkwave.auth.security.jwt.JwtRefreshParser;
import org.linkwave.auth.security.jwt.JwtRefreshSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFiltersConfigurer jwtAuthFiltersConfigurer;
    private final UserDetailsService userDetailsService;

    @Value("classpath:keys/refresh_private_key.pem")
    private RSAPrivateKey refreshPrivateKey;

    @Value("classpath:keys/refresh_public_key.pem")
    private RSAPublicKey refreshPublicKey;

    @Value("classpath:keys/access_private_key.pem")
    private RSAPrivateKey accessPrivateKey;

    @Value("classpath:keys/access_public_key.pem")
    private RSAPublicKey accessPublicKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Bean
    public SecurityFilterChain filterChain(@NonNull HttpSecurity httpSecurity,
                                           AuthenticationManager authenticationManager,
                                           JwtRefreshSerializer refreshSerializer,
                                           JwtAccessSerializer accessSerializer,
                                           JwtRefreshParser refreshParser,
                                           JwtAccessParser accessParser) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .userDetailsService(userDetailsService)

                .authorizeHttpRequests(c -> c
                        .requestMatchers("/actuator/health", GET.name()).permitAll()
                        .requestMatchers("/api/v1/auth/validate-token", GET.name()).permitAll()
                        .anyRequest().denyAll())

                .apply(jwtAuthFiltersConfigurer)
                .setAuthenticationManager(authenticationManager)
                .setJwtRefreshSerializer(refreshSerializer)
                .setJwtAccessSerializer(accessSerializer)
                .setJwtRefreshParser(refreshParser)
                .setJwtAccessParser(accessParser);


        final var filterChain = httpSecurity.build();
        filterChain.getFilters().stream()
                .map(filter -> filter.getClass().getSimpleName())
                .forEach(name -> log.info("[*] {}", name));

        return filterChain;
    }

    @Bean
    public AuthenticationManager authenticationManager(@NonNull AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtRefreshSerializer jwtRefreshSerializer() {
        return new JwtRefreshSerializer(issuer, refreshPrivateKey);
    }

    @Bean
    public JwtAccessSerializer jwtAccessSerializer() {
        return new JwtAccessSerializer(issuer, accessPrivateKey);
    }

    @Bean
    public JwtRefreshParser jwtRefreshParser() {
        return new JwtRefreshParser(refreshPublicKey);
    }

    @Bean
    public JwtAccessParser jwtAccessParser() {
        return new JwtAccessParser(accessPublicKey);
    }

}
