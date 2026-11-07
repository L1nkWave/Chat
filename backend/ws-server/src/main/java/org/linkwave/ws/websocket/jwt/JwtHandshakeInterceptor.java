package org.linkwave.ws.websocket.jwt;

import org.linkwave.ws.websocket.dto.ErrorMessage;
import org.linkwave.ws.websocket.ticket.WsTicketService;
import tools.jackson.databind.ObjectMapper;
import org.linkwave.shared.auth.TokenParser;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.dto.Action;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Accepts a WebSocket handshake only with a valid single-use ticket
 * (obtained from POST /api/v1/ws-tickets). The access token itself is never
 * accepted in the URL or in headers at this point.
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TICKET_PARAM_KEY = "ticket";
    public static final String TOKEN_ATTRIBUTE_KEY = "access-token";
    public static final String CONTENT_HEADER_KEY = "Content";
    public static final String HANDSHAKE_PATH = "/ws";

    private final TokenParser tokenParser;
    private final WsTicketService ticketService;
    private final ObjectMapper objectMapper;

    public JwtHandshakeInterceptor(TokenParser tokenParser,
                                   WsTicketService ticketService,
                                   @Lazy ObjectMapper objectMapper) {
        this.tokenParser = tokenParser;
        this.ticketService = ticketService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {

        final String ticket = ((ServletServerHttpRequest) request).getServletRequest()
                .getParameter(TICKET_PARAM_KEY);

        if (ticket == null || ticket.isBlank()) {
            reject(response, "ticket is missing");
            return false;
        }

        // GETDEL: the ticket is gone after this call, so it cannot be replayed
        final Optional<String> token = ticketService.consume(ticket);
        if (token.isEmpty()) {
            reject(response, "ticket is invalid or expired");
            return false;
        }

        if (tokenParser.isInvalid(token.get())) {
            reject(response, "invalid access token");
            return false;
        }

        attributes.put(TOKEN_ATTRIBUTE_KEY, token.get());
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private void reject(@NonNull ServerHttpResponse response, @NonNull String error) {
        log.debug("-> beforeHandshake(): {}", error);
        fillResponse(response, error);
    }

    @SneakyThrows
    private void fillResponse(@NonNull ServerHttpResponse response, @NonNull String error) {
        final var servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        final var jsonApiError = objectMapper.writeValueAsString(ErrorMessage.builder()
                .path(HANDSHAKE_PATH)
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