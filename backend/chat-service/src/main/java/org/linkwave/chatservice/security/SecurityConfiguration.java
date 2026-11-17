package org.linkwave.chatservice.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.linkwave.shared.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.time.Instant;
import org.linkwave.shared.auth.BearerAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.security.interfaces.RSAPublicKey;

@Configuration
public class SecurityConfiguration {

    @Value("classpath:keys/access_public_key.pem")
    private RSAPublicKey publicKey;

    @Bean
    public SecurityFilterChain filterChain(@NonNull HttpSecurity httpSecurity,
                                           JwtAuthConfigurer jwtAuthConfigurer,
                                           AuthenticationConverter authenticationConverter,
                                           ObjectMapper objectMapper) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(matcherRegistry -> matcherRegistry
                        .requestMatchers("/api/v*/chats/**").hasRole("USER")
                        .requestMatchers("/api/v*/moderation/**").hasRole("USER")
                        .anyRequest().permitAll())

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> writeError(
                                objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                                "Thiếu hoặc sai access token"))
                        .accessDeniedHandler((request, response, ex) -> writeError(
                                objectMapper, request, response, HttpStatus.FORBIDDEN,
                                "Không có quyền truy cập")))

                .with(jwtAuthConfigurer, configurer -> configurer
                        .setAuthenticationConverter(authenticationConverter)
                        .setObjectMapper(objectMapper))

                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(@NonNull AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public AuthenticationConverter authenticationConverter() {
        return new BearerAuthenticationConverter(publicKey);
    }

    private static void writeError(ObjectMapper objectMapper, HttpServletRequest request,
                                   HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                request.getRequestURI(), message, status.value(), Instant.now()));
    }

}