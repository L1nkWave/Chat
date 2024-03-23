package org.linkwave.chatservice.api.ws;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(value = "ws-server", path = "/api/v1/ws-chats")
public interface WSServiceClient {

    @PostMapping
    boolean loadNewChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @RequestBody LoadChatRequest loadChatRequest
    );

    @PostMapping("/group/{chatId}")
    boolean loadNewGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

}
