package org.linkwave.ws.api.chat;

import org.linkwave.ws.websocket.dto.client.NewGroupChat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(value = "chat-service", path = "/api/v1/chats")
public interface ChatServiceClient {

    @GetMapping("/ids")
    List<String> getUserChats(@RequestHeader(AUTHORIZATION) String authHeader);

    @PostMapping
    void createChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @RequestBody NewChatRequest chatRequest
    );

    @PostMapping("/group")
    GroupChatDto createGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @RequestBody NewGroupChat body
    );

    @PostMapping("/{chatId}/messages/text")
    MessageDto saveTextMessage(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestBody NewTextMessage message
    );

    @PostMapping("/{chatId}/group/members")
    void joinGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @DeleteMapping("/{chatId}/group/members")
    void leaveGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @GetMapping("/{chatId}/group/member")
    void isGroupChatMember(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @GetMapping("/{chatId}/exists")
    void isMember(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestParam Long recipientId
    );

}
