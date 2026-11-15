package org.linkwave.chatservice.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageRevisionService {

    private final MessageRevisionRepository repository;

    /**
     * Lưu bản gốc của tin nhắn.
     * @param authorId người gửi tin gốc
     * @param actorId người thực hiện hành động (sửa, xóa, hoặc chính người gửi khi bị chặn)
     */
    public void record(RevisionAction action, String chatId, String messageId,
                       Long authorId, Long actorId, String text) {
        repository.save(MessageRevision.builder()
                .action(action)
                .chatId(chatId)
                .messageId(messageId)
                .authorId(authorId)
                .actorId(actorId)
                .text(text)
                .createdAt(Instant.now())
                .build());
    }

}