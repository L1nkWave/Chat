package com.chat.wsserver.websocket.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.AbstractHandshakeHandler;

import java.security.Principal;
import java.util.Map;

import static com.chat.wsserver.websocket.jwt.JwtHandshakeInterceptor.BEARER_PREFIX;
import static com.chat.wsserver.websocket.jwt.JwtHandshakeInterceptor.JWT_HEADER_KEY;

@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends AbstractHandshakeHandler {

    private final TokenParser tokenParser;

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                      @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {

        final String rawToken = request.getHeaders()
                .get(JWT_HEADER_KEY)
                .get(0)
                .substring(BEARER_PREFIX.length());

        return new UserPrincipal(rawToken, tokenParser.parse(rawToken));
    }

}
