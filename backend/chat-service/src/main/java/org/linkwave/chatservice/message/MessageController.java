package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @JsonView(MessageDto.Created.class)
    @PostMapping
    @ResponseStatus(CREATED)
    public MessageDto saveMessage(@PathVariable String chatId,
                                  @RequestParam Action action) {
        return messageService.saveMessage(userDetails().id(), chatId, action);
    }

    @JsonView(MessageDto.Created.class)
    @PostMapping("/text")
    @ResponseStatus(CREATED)
    public MessageDto saveTextMessage(@PathVariable String chatId,
                                      @Valid @RequestBody NewTextMessage message) {
        return messageService.saveTextMessage(userDetails().id(), chatId, message);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getMessages(@PathVariable String chatId) {
        return ok(messageService.getChatMessages(userDetails().id(), chatId));
    }

}
