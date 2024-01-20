package org.linkwave.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.apigateway.dto.ApiError;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationGatewayFilter implements GatewayFilter {

    public static final String BEARER_PREFIX = "Bearer ";

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

        return webClient.get()
                .uri("/api/v1/auth/validate-token")
                .header(AUTHORIZATION, header.get(0))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return chain.filter(exchange);
                    }
                    final Mono<String> body = response.bodyToMono(String.class);
                    return fillResponse(exchange.getResponse(), body, (HttpStatus) response.statusCode());
                });
    }

    @NonNull
    private Mono<Void> fillResponse(@NonNull ServerHttpResponse response,
                                    @NonNull Mono<String> body,
                                    @NonNull HttpStatus status) {
        return response.writeWith(body.map(str -> {
            final HttpHeaders headers = response.getHeaders();
            final byte[] bytes = str.getBytes(UTF_8);

            response.setStatusCode(status);
            headers.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
            headers.add(CONTENT_LENGTH, String.valueOf(bytes.length));

            return response.bufferFactory().wrap(bytes);
        }));
    }

}
