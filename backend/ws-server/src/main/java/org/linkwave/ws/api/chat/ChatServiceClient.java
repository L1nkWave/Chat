package org.linkwave.ws.api.chat;

import org.linkwave.ws.websocket.dto.client.NewGroupChat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "chat-service", path = "/api/v1/chats")
public interface ChatServiceClient {

    @GetMapping("/ids")
    List<String> getUserChats(@RequestHeader("Authorization") String authHeader);

    @PostMapping
    void createChat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody NewChatRequest chatRequest
    );

    @PostMapping("/group")
    GroupChatDto createGroupChat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody NewGroupChat body
    );

    @PostMapping("/{chatId}/messages/text")
    MessageDto saveTextMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String chatId,
            @RequestBody NewTextMessage message
    );

    @PostMapping("/{chatId}/group/members")
    void joinGroupChat(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String chatId
    );

    @DeleteMapping("/{chatId}/group/members")
    void leaveGroupChat(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String chatId
    );

}
