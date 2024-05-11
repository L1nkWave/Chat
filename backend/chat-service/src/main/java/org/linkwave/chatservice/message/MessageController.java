package org.linkwave.chatservice.message;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.common.DtoViews.New;
import org.linkwave.chatservice.common.UnacceptableRequestDataException;
import org.linkwave.chatservice.message.file.CreatedFileMessage;
import org.linkwave.chatservice.message.text.EditTextMessage;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.linkwave.chatservice.message.text.UpdatedTextMessage;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.*;

@Slf4j
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

    @PostMapping("/{chatId}/messages/file")
    @ResponseStatus(CREATED)
    public CreatedFileMessage saveFileMessage(@PathVariable String chatId,
                                              @NonNull HttpServletRequest request,
                                              @NonNull @RequestPart("file") MultipartFile[] files) {
        if (files.length > 1) {
            throw new UnacceptableRequestDataException("Only one file can be uploaded per request");
        }

        final MultipartFile targetFile = files[0];
        if (targetFile.isEmpty()) {
            throw new UnacceptableRequestDataException("Uploaded file can not be empty");
        }

        return messageService.saveFileMessage(requestInitiator(request), chatId, targetFile);
    }

    @GetMapping("/{chatId}/messages/file/own")
    public ResponseEntity<Void> isOwnFileMessage(@PathVariable String chatId,
                                                 @RequestParam String messageId,
                                                 @RequestParam String filename,
                                                 @RequestParam String contentType,
                                                 @RequestParam long size) {
        final CreatedFileMessage message = CreatedFileMessage.builder()
                .id(messageId)
                .filename(filename)
                .contentType(contentType)
                .size(size)
                .build();
        final boolean isOwn = messageService.isOwnFileMessage(userDetails().id(), chatId, message);
        return ResponseEntity.status(isOwn ? OK : NOT_FOUND).build();
    }

    @GetMapping("/messages/{messageId}/file")
    public byte[] getAttachedFile(@PathVariable String messageId) {
        return messageService.getAttachedFile(userDetails().id(), messageId);
    }

    @GetMapping("/{chatId}/messages")
    public Map<LocalDate, List<MessageDto>> getMessages(
            @PathVariable String chatId, @RequestParam int offset, @RequestParam int limit,
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response
    ) {
        log.debug("-> getMessages(): offset = {}, limit={}", offset, limit);

        final Pair<Long, Map<LocalDate, List<MessageDto>>> chatMessages = messageService.getChatMessages(
                requestInitiator(request), chatId, offset, limit
        );
        response.addHeader(TOTAL_COUNT.getValue(), String.valueOf(chatMessages.getFirst()));
        return chatMessages.getSecond();
    }

    @PatchMapping("/messages/{id}/text")
    public UpdatedTextMessage editTextMessage(@PathVariable String id,
                                              @Valid @RequestBody EditTextMessage editMessage) {
        return messageService.editTextMessage(userDetails().id(), id, editMessage);
    }

    @DeleteMapping("/messages/{id}")
    public RemovedMessage removeMessage(@PathVariable String id) {
        return messageService.removeMessage(userDetails().id(), id);
    }

    @DeleteMapping("/{chatId}/messages")
    @ResponseStatus(NO_CONTENT)
    public void clearMessages(@PathVariable String chatId) {
        messageService.clearMessages(userDetails().id(), chatId);
    }

    @PatchMapping("/{chatId}/messages/readers")
    public ReadMessages readMessages(@PathVariable String chatId,
                                     @RequestParam("to") Instant timestamp) {
        return messageService.readMessages(userDetails().id(), chatId, timestamp);
    }

}
