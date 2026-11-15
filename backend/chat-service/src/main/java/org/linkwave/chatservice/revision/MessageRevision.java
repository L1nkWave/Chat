package org.linkwave.chatservice.revision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Bản gốc của tin nhắn trước khi bị sửa, xóa, hoặc bị hệ thống chặn.
 * Dùng làm bằng chứng khi xử lý báo cáo. Tự xóa sau 90 ngày.
 */
@Document("message_revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRevision {

    @Id
    private String id;
    private RevisionAction action;
    private String chatId;
    private String messageId;
    private Long authorId;
    private Long actorId;
    private String text;

    @Indexed(expireAfter = "P90D")
    private Instant createdAt;

}