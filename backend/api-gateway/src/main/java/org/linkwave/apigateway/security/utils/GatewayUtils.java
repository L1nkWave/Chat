package org.linkwave.apigateway.security.utils;

import lombok.experimental.UtilityClass;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@UtilityClass
public class GatewayUtils {

    public static final String BEARER_PREFIX = "Bearer ";

    @NonNull
    public static Mono<Void> validateToken(@NonNull ServerWebExchange exchange,
                                           @NonNull GatewayFilterChain chain,
                                           @NonNull WebClient webClient,
                                           String token) {
        return webClient.get()
                .uri("/api/v1/auth/validate-token")
                .header(AUTHORIZATION, token)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return chain.filter(exchange);
                    }
                    final Mono<String> body = response.bodyToMono(String.class);
                    return fillResponse(exchange.getResponse(), body, (HttpStatus) response.statusCode());
                });
    }

    @NonNull
    public static Mono<Void> fillResponse(@NonNull ServerHttpResponse response,
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
