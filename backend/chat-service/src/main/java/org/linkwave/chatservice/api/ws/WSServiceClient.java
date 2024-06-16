package org.linkwave.chatservice.api.ws;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(value = "ws-server", path = "/api/v1/ws-chats")
public interface WSServiceClient {

    @PostMapping
    void loadNewChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @RequestBody LoadChatRequest loadChatRequest
    );

    @PostMapping("/group/{chatId}")
    void loadNewGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @PatchMapping("/{id}/unread_messages")
    void addUnreadMessage(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String id,
            @RequestParam Long senderId
    );

}
