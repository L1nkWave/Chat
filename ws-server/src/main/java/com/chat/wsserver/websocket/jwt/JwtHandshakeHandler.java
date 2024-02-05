package com.chat.wsserver.websocket.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.AbstractHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static com.chat.wsserver.websocket.jwt.JwtHandshakeInterceptor.TOKEN_PARAM_KEY;

@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends AbstractHandshakeHandler {

    private final TokenParser tokenParser;

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                      @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {

        final var token = Optional.ofNullable(attributes.get(TOKEN_PARAM_KEY))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("Not found access token"));

        return new UserPrincipal(token, tokenParser.parse(token));
    }

}
