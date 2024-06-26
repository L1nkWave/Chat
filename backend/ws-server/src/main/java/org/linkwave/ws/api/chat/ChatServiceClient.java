package org.linkwave.ws.api.chat;

import org.linkwave.ws.websocket.dto.NewGroupChat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.NO_CONTENT;

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

    @DeleteMapping("/{id}/group")
    void removeGroupChat(@RequestHeader(AUTHORIZATION) String authHeader, @PathVariable String id);

    @PostMapping("/{chatId}/messages/text")
    MessageDto saveTextMessage(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestBody NewTextMessage message
    );

    @PostMapping("/{chatId}/group/members")
    ChatMemberDto joinGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @DeleteMapping("/{chatId}/group/members")
    void leaveGroupChat(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId
    );

    @PostMapping("/{chatId}/group/members/{userId}")
    ChatMemberDto addGroupChatMember(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @PathVariable Long userId
    );

    @DeleteMapping("/{chatId}/group/members/{memberId}")
    ChatMemberDto removeGroupChatMember(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @PathVariable Long memberId
    );

    @PatchMapping("/{chatId}/group/members/{memberId}/roles")
    void changeMemberRole(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId, @PathVariable Long memberId,
            @RequestParam ChatRole role
    );

    @GetMapping("/members/batch")
    Map<String, Set<ChatMember>> getChatsMembers(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @RequestParam List<String> ids
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

    @PatchMapping("/messages/{messageId}/text")
    UpdatedTextMessage editTextMessage(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String messageId,
            @RequestBody NewTextMessage message
    );

    @PostMapping("/{chatId}/messages/file")
    CreatedFileMessage uploadFile(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestPart("file") MultipartFile[] file
    );

    @GetMapping("/{chatId}/messages/file/own")
    void isOwnFileMessage(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestParam String messageId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @RequestParam long size
    );

    @DeleteMapping("/messages/{id}")
    RemovedMessage removeMessage(@RequestHeader(AUTHORIZATION) String authHeader, @PathVariable String id);

    @DeleteMapping("/{chatId}/messages")
    void clearMessages(@RequestHeader(AUTHORIZATION) String authHeader, @PathVariable String chatId);

    @PatchMapping("/{chatId}/messages/readers")
    ReadMessages readMessages(
            @RequestHeader(AUTHORIZATION) String authHeader,
            @PathVariable String chatId,
            @RequestParam("to") Instant timestamp
    );

}
