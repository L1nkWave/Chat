package org.linkwave.ws.websocket.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.websocket.routing.args.ArgumentResolverStrategy;
import org.linkwave.ws.websocket.routing.args.PathVariableResolverStrategy;
import org.linkwave.ws.websocket.routing.args.PayloadResolverStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;

import java.security.Principal;
import java.util.List;

@Configuration
public class RoutingAutoConfig {

    public static final String PATH_PARAM_NAME = "path";

    // argument resolvers registry
    @Bean
    public List<ArgumentResolverStrategy> argumentResolverStrategies(ObjectMapper objectMapper) {
        return List.of(
                new PathVariableResolverStrategy(),
                new PayloadResolverStrategy(objectMapper),

                // inject ws-session
                (ctx, param) -> param.getType().equals(WebSocketSession.class) ? ctx.session() : null,

                // inject user principal
                (ctx, param) -> param.getType().equals(Principal.class) ||
                                param.getType().equals(UserPrincipal.class)
                        ? ctx.session().getPrincipal()
                        : null,

                // inject path
                (ctx, param) -> param.getType().equals(String.class) &&
                                param.getName().equals(PATH_PARAM_NAME)
                        ? ctx.routingMessage().path()
                        : null,

                // inject primitive value
                (ctx, param) ->
                        param.getType().isPrimitive()
                                ? param.getType().equals(boolean.class) ? false : 0
                                : null

        );
    }

}
