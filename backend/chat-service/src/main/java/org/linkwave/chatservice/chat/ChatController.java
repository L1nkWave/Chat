package org.linkwave.chatservice.chat;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.ChatDto;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.GroupChatDetailsDto;
import org.linkwave.chatservice.chat.group.GroupChatDto;
import org.linkwave.chatservice.chat.group.NewGroupChatRequest;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.linkwave.chatservice.common.DtoViews.New;
import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public List<ChatDto> getUserChats(@RequestParam int offset, @RequestParam int limit,
                                      @NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response) {

        log.debug("-> getUserChats(): offset = {}, limit={}", offset, limit);

        final Pair<Long, List<ChatDto>> userChats = chatService.getUserChats(
                requestInitiator(request),
                offset, limit
        );
        response.addHeader(TOTAL_COUNT.getValue(), String.valueOf(userChats.getFirst()));
        return userChats.getSecond();
    }

    @GetMapping("/ids")
    public List<String> getUserChats() {
        return chatService.getUserChats(userDetails().id());
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @JsonView(New.class)
    public ChatDto createChat(@RequestBody @Valid NewChatRequest chatRequest,
                              @NonNull HttpServletRequest request) {
        return chatService.createChat(requestInitiator(request), chatRequest);
    }

    @GetMapping("/{id}/exists")
    @ResponseStatus(NO_CONTENT)
    public void checkChatPair(@PathVariable String id,
                              @RequestParam Long recipientId) {
        final Chat chat = chatService.findChat(id);
        if (chatService.isMember(userDetails().id(), chat) &&
            chatService.isMember(recipientId, chat)) {
            return;
        }
        throw new ResourceNotFoundException("Membership is not confirmed");
    }

    @PostMapping("/group")
    @ResponseStatus(CREATED)
    @JsonView(New.class)
    public GroupChatDto createGroupChat(@RequestBody @Valid NewGroupChatRequest chatRequest,
                                        @NonNull HttpServletRequest request) {
        return chatService.createGroupChat(requestInitiator(request), chatRequest);
    }

    @GetMapping("/{id}/group")
    public GroupChatDetailsDto getGroupChat(@PathVariable String id,
                                            @NonNull HttpServletRequest request) {
        return chatService.getGroupChatDetails(requestInitiator(request), id);
    }

    @GetMapping("/{id}/group/member")
    @ResponseStatus(NO_CONTENT)
    public void isGroupChatMember(@PathVariable String id) {
        if (!chatService.isMember(userDetails().id(), chatService.findGroupChat(id))) {
            throw new ResourceNotFoundException("Membership is not confirmed");
        }
    }

    @GetMapping("/members/batch")
    public Map<String, List<ChatMember>> getChatsMembers(@RequestParam List<String> ids) {
        return chatService.getChatsMembers(userDetails().id(), ids);
    }

    @PostMapping("/{id}/group/members")
    @ResponseStatus(CREATED)
    public ChatMemberDto joinGroupChat(@PathVariable String id, @NonNull HttpServletRequest request) {
        return chatService.addGroupChatMember(id, requestInitiator(request));
    }

    @DeleteMapping("/{id}/group/members")
    @ResponseStatus(NO_CONTENT)
    public void leaveGroupChat(@PathVariable String id) {
        chatService.removeGroupChatMember(userDetails().id(), id);
    }

    @PostMapping("/{id}/group/members/{userId}")
    @ResponseStatus(CREATED)
    public ChatMemberDto addMember(@PathVariable String id, @PathVariable Long userId,
                                   @NonNull HttpServletRequest request) {
        return chatService.addGroupChatMember(id, requestInitiator(request), userId);
    }

    @DeleteMapping("/{id}/group/members/{memberId}")
    public ChatMemberDto removeMember(@PathVariable String id, @PathVariable Long memberId,
                                      @NonNull HttpServletRequest request) {
        return chatService.removeGroupChatMember(id, requestInitiator(request), memberId);
    }

    @PatchMapping("/{id}/group/members/{memberId}/roles")
    public void changeMemberRole(@PathVariable String id,
                                 @PathVariable Long memberId,
                                 @RequestParam ChatRole role) {
        chatService.changeMemberRole(id, userDetails().id(), memberId, role);
    }

    @PostMapping("/{id}/group/avatar")
    @ResponseStatus(CREATED)
    public void uploadGroupChatAvatar(@PathVariable String id,
                                      @NonNull @RequestPart("file") MultipartFile[] files) {

        log.debug("-> uploadAvatar(): [{}] files", files.length);
        if (files.length > 1) {
            throw new UnacceptableRequestDataException("Only one file can be uploaded per request");
        }

        final MultipartFile targetFile = files[0];
        if (targetFile.isEmpty()) {
            throw new UnacceptableRequestDataException("Uploaded file can not be empty");
        }
        chatService.changeGroupChatAvatar(id, targetFile);
    }

    @GetMapping(
            value = "/{id}/group/avatar",
            produces = {
                    IMAGE_PNG_VALUE,
                    IMAGE_GIF_VALUE,
                    IMAGE_JPEG_VALUE,
                    "image/jpg",
                    "image/webp"
            }
    )
    public byte[] getGroupChatAvatar(@PathVariable String id) {
        return chatService.getGroupChatAvatar(id);
    }

    @DeleteMapping("{id}/group/avatar")
    @ResponseStatus(OK)
    public void deleteGroupChatAvatar(@PathVariable String id) {
        chatService.deleteGroupChatAvatar(id);
    }

}
