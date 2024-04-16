package org.linkwave.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linkwave.shared.auth.BearerAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.security.interfaces.RSAPublicKey;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Value("classpath:keys/access_public_key.pem")
    private RSAPublicKey publicKey;

    @Bean
    public SecurityFilterChain filterChain(@NonNull HttpSecurity httpSecurity,
                                           JwtAuthConfigurer jwtAuthConfigurer,
                                           AuthenticationConverter authenticationConverter,
                                           ObjectMapper objectMapper) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(matcherRegistry -> matcherRegistry
                        .requestMatchers("/api/v1/users/register", POST.name()).permitAll()
                        .requestMatchers("/actuator/health", GET.name()).permitAll()
                        .requestMatchers("/api/v1/users/{id}/online").permitAll()
                        .requestMatchers("/api/v*/users/**").hasRole("USER")
                        .anyRequest().authenticated())

                .apply(jwtAuthConfigurer)
                .setAuthenticationConverter(authenticationConverter)
                .setObjectMapper(objectMapper);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationConverter authenticationConverter() {
        return new BearerAuthenticationConverter(publicKey);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}