package org.linkwave.ws.websocket.ticket;

import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.TokenParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ws-tickets")
public class WsTicketController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenParser tokenParser;
    private final WsTicketService ticketService;

    /**
     * Issues a single-use WebSocket ticket for the authenticated user.
     * The access token is validated here as well, so this endpoint is safe even when
     * it is reached without the gateway.
     */
    @PostMapping
    public ResponseEntity<WsTicketResponse> issue(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer is not present");
        }

        final String token = authorization.substring(BEARER_PREFIX.length());
        if (tokenParser.isInvalid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid access token");
        }

        final String ticket = ticketService.issue(token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new WsTicketResponse(ticket, WsTicketService.TTL_SECONDS));
    }

    public record WsTicketResponse(String ticket, int expiresIn) {
    }

}