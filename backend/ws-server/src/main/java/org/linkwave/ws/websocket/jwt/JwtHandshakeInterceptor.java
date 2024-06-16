package org.linkwave.ws.websocket.jwt;

import org.linkwave.ws.websocket.dto.ErrorMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.linkwave.shared.auth.TokenParser;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.dto.Action;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TOKEN_HEADER_KEY = HttpHeaders.AUTHORIZATION;
    public static final String TOKEN_PARAM_KEY = "access";
    public static final String CONTENT_HEADER_KEY = "Content";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenParser tokenParser;
    private final ObjectMapper objectMapper;

    public JwtHandshakeInterceptor(TokenParser tokenParser, @Lazy ObjectMapper objectMapper) {
        this.tokenParser = tokenParser;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {

        final List<String> authorizationHeader = request.getHeaders().get(TOKEN_HEADER_KEY);
        final String error;
        final String token;

        // check header & token format
        if (authorizationHeader != null && !authorizationHeader.isEmpty()) {

            final String headerValue = authorizationHeader.get(0);
            if (!headerValue.startsWith(BEARER_PREFIX)) {
                error = format("Header \"%s\" contains invalid bearer format", TOKEN_HEADER_KEY);
                log.debug("-> beforeHandshake(): {}", error);
                fillResponse(response, error);
                return false;
            }

            token = headerValue.substring(BEARER_PREFIX.length());
        } else { // check parameter & token format
            final String paramValue = ((ServletServerHttpRequest) request).getServletRequest().getParameter(TOKEN_PARAM_KEY);

            if (paramValue == null || paramValue.isBlank()) {
                error = format("Parameter \"%s\" is empty", TOKEN_PARAM_KEY);
                log.debug("-> beforeHandshake(): {}", error);
                fillResponse(response, error);
                return false;
            }

            token = paramValue;
        }

        if (tokenParser.isInvalid(token)) {
            error = "invalid access token";
            log.debug("-> beforeHandshake(): {}", error);
            fillResponse(response, error);
            return false;
        }

        attributes.put(TOKEN_PARAM_KEY, token);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
    }

    @SneakyThrows
    private void fillResponse(@NonNull ServerHttpResponse response, @NonNull String error) {
        final var servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        final var jsonApiError = objectMapper.writeValueAsString(ErrorMessage.builder()
                .path("/ws-gate")
                .error(error)
                .action(Action.ERROR)
                .build());

        // temporary solution to write json into response header and body
        servletResponse.setStatus(UNAUTHORIZED.value());
        servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        servletResponse.setHeader(CONTENT_HEADER_KEY, jsonApiError);

        @Cleanup final var outputStream = servletResponse.getOutputStream();
        outputStream.write(jsonApiError.getBytes());
        outputStream.flush();
    }

}
