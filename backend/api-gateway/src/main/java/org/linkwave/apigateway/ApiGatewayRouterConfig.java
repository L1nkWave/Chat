package org.linkwave.apigateway;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

import static org.linkwave.apigateway.ApiGatewayRouterConfig.Service.*;
import static org.springframework.http.HttpMethod.*;

@Configuration
public class ApiGatewayRouterConfig {

    @Getter
    @RequiredArgsConstructor
    public enum Service {
        AUTH_SERVICE("lb://auth-service"),
        USER_SERVICE("lb://user-service"),
        CHAT_SERVICE("lb://chat-service"),
        WS_SERVER("lb:ws://ws-server");

        private final String url;
    }

    @Bean
    public RouteLocator routeLocator(@NonNull RouteLocatorBuilder builder,
                                     @Qualifier("authHeaderFilter") GatewayFilter authHeaderGatewayFilter) {
        return builder.routes()
                .route(r -> r
                        .path(
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/refresh-tokens")
                        .and()
                        .method(POST, GET, OPTIONS)
                        .uri(AUTH_SERVICE.getUrl())
                )
                .route(r -> r
                        .path("/api/v1/users/register")
                        .and()
                        .method(POST, OPTIONS)
                        .uri(USER_SERVICE.getUrl()))
                .route(r -> r
                        .path("/api/v1/users/**")
                        .and()
                        .not(fn -> fn.path("/api/v1/users/{id}/online"))
                        .filters(f -> f.filter(authHeaderGatewayFilter))
                        .uri(USER_SERVICE.getUrl()))
                .route(r -> r
                        .path("/api/v1/chats/**")
                        .filters(f -> f.filter(authHeaderGatewayFilter))
                        .uri(CHAT_SERVICE.getUrl()))
                .route(r -> r
                        .path("/api/v1/ws-tickets")
                        .and()
                        .method(POST)
                        .filters(f -> f.filter(authHeaderGatewayFilter))
                        .uri(WS_SERVER.getUrl()))
                // handshake is authorized by the single-use ticket inside ws-server, not by the gateway
                .route(r -> r
                        .path("/ws")
                        .uri(WS_SERVER.getUrl()))
                .build();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient webClient(@NonNull WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(AUTH_SERVICE.getUrl()).build();
    }

}