package com.chat.wsserver.websocket.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.AbstractHandshakeHandler;

import java.security.Principal;
import java.util.Map;

import static com.chat.wsserver.websocket.jwt.UserPrincipal.JWT_HEADER_KEY;

@Slf4j
@Component
public class JwtHandshakeHandler extends AbstractHandshakeHandler {

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                      @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {
        log.debug("-> determineUser()");

        String jwt = request.getHeaders().get(JWT_HEADER_KEY).get(0);

        return new UserPrincipal(jwt);
    }

}
