package org.linkwave.ws.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.ws.repository.ChatRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/api/v1/ws-chats")
@RequiredArgsConstructor
public class WSChatsController {

    private final ChatRepository<Long, String> chatRepository;

    @PostMapping
    @ResponseStatus(CREATED)
    public void loadNewChat(@RequestBody @Valid LoadChatRequest request) {
        log.debug("-> loadChat()");
        chatRepository.addMember(userDetails().id(), request.getChatId());
        chatRepository.addMember(request.getRecipientId(), request.getChatId());
    }

    @PostMapping("/group/{id}")
    @ResponseStatus(CREATED)
    public void loadNewGroupChat(@PathVariable String id) {
        log.debug("-> loadGroupChat()");
        chatRepository.addMember(userDetails().id(), id);
    }

    private DefaultUserDetails userDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

}
