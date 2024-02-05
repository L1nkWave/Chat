package org.linkwave.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.apigateway.dto.ApiError;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.linkwave.apigateway.security.utils.GatewayUtils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Component("authHeaderFilter")
@RequiredArgsConstructor
public class AuthenticationHeaderGatewayFilter implements GatewayFilter {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        log.debug("-> filter()");
        final List<String> header = exchange.getRequest()
                .getHeaders()
                .get(AUTHORIZATION);

        if (header == null || !header.get(0).startsWith(BEARER_PREFIX)) {
            final var response = exchange.getResponse();
            final String error = objectMapper.writeValueAsString(ApiError.builder()
                    .path(exchange.getRequest().getPath().value())
                    .timestamp(Instant.now())
                    .status(BAD_REQUEST.value())
                    .message("Bearer is not present")
                    .build());

            return fillResponse(response, Mono.just(error), BAD_REQUEST);
        }

        return validateToken(exchange, chain, webClient, header.get(0));
    }

}
