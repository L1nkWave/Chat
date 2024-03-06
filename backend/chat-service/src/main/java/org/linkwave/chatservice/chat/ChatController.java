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
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public List<ChatDto> getUserChats(@RequestParam int offset, @RequestParam int limit,
                                      @NonNull HttpServletResponse response) {

        log.debug("-> getUserChats(): offset = {}, limit={}", offset, limit);

        final Pair<Long, List<ChatDto>> userChats = chatService.getUserChats(userDetails().id(), offset, limit);
        response.addHeader(TOTAL_COUNT.getValue(), String.valueOf(userChats.getFirst()));
        return userChats.getSecond();
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
    public GroupChatDetailsDto getGroupChat(@PathVariable String id) {
        return chatService.getGroupChatDetails(userDetails().id(), id);
    }

}
