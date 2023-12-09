package com.chat.wsserver.websocket.jwt;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

import static com.chat.wsserver.websocket.jwt.UserPrincipal.JWT_HEADER_KEY;
import static java.lang.String.format;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @SneakyThrows
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {

        log.debug("-> beforeHandshake()");

        HttpHeaders headers = request.getHeaders();
        List<String> values = headers.get(JWT_HEADER_KEY);

        String error;

        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            error = format("%s header undefined", JWT_HEADER_KEY);
            log.debug("-> beforeHandshake(): {}", error);

            setResponseError(response, error);
            return false;
        }

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private void setResponseError(ServerHttpResponse response, String error) {
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Error", error);
    }

}
