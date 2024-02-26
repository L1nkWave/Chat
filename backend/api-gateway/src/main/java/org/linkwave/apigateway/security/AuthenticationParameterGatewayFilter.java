package org.linkwave.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.dto.ApiError;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.linkwave.apigateway.security.utils.GatewayUtils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Component("authParamFilter")
@RequiredArgsConstructor
public class AuthenticationParameterGatewayFilter implements GatewayFilter {

    public static final String TOKEN_PARAM_KEY = "access";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        log.debug("-> filter()");

        final List<String> parameter = exchange.getRequest().getQueryParams().get(TOKEN_PARAM_KEY);

        if (parameter == null || parameter.isEmpty() || parameter.get(0).isBlank()) {
            log.debug("-> filter(): token not found");
            final var response = exchange.getResponse();
            final String error = objectMapper.writeValueAsString(ApiError.builder()
                    .path(exchange.getRequest().getPath().value())
                    .timestamp(Instant.now())
                    .status(BAD_REQUEST.value())
                    .message("Bearer is not present")
                    .build());

            return fillResponse(response, Mono.just(error), BAD_REQUEST);
        }

        return validateToken(exchange, chain, webClient, String.format("%s%s", BEARER_PREFIX, parameter.get(0)));
    }

}
