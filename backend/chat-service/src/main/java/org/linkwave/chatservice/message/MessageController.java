package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.chatservice.common.DtoViews.New;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @JsonView(New.class)
    @PostMapping("/{chatId}/messages")
    @ResponseStatus(CREATED)
    public MessageDto saveMessage(@PathVariable String chatId,
                                  @RequestParam Action action) {
        return messageService.saveMessage(userDetails().id(), chatId, action);
    }

    @JsonView(New.class)
    @PostMapping("/{chatId}/messages/text")
    @ResponseStatus(CREATED)
    public MessageDto saveTextMessage(@PathVariable String chatId,
                                      @Valid @RequestBody NewTextMessage message) {
        return messageService.saveTextMessage(userDetails().id(), chatId, message);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable String chatId) {
        return ok(messageService.getChatMessages(userDetails().id(), chatId));
    }

    @PostMapping("/{chatId}/messages/readers")
    public List<String> readMessages(@PathVariable String chatId,
                                     @RequestParam("to") String lastReadMessageId) {
        return messageService.readMessages(userDetails().id(), chatId, lastReadMessageId);
    }

}
