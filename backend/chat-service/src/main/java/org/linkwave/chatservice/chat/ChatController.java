package org.linkwave.chatservice.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.chat.duo.ChatDto;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.GroupChatDetailsDto;
import org.linkwave.chatservice.chat.group.GroupChatDto;
import org.linkwave.chatservice.chat.group.NewGroupChatRequest;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
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
    public ChatDto createChat(@RequestBody @Valid NewChatRequest chatRequest,
                              @NonNull HttpServletRequest request) {
        return chatService.createChat(requestInitiator(request), chatRequest);
    }

    @PostMapping("/group")
    @ResponseStatus(CREATED)
    public GroupChatDto createGroupChat(@RequestBody @Valid NewGroupChatRequest chatRequest) {
        return chatService.createGroupChat(userDetails().id(), chatRequest);
    }

    @GetMapping("/{id}/group")
    public GroupChatDetailsDto getGroupChat(@PathVariable String id,
                                            @NonNull HttpServletRequest request) {
        return chatService.getGroupChatDetails(requestInitiator(request), id);
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
