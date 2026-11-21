package org.linkwave.chatservice.message.file;

import java.time.Instant;

/**
 * Một mục trong thư viện ảnh / file. Tệp đã xóa vẫn được trả về, {@code removedAt} khác null để client hiển thị placeholder.
 */
public record MediaItemDto(
        String messageId,
        String filename,
        String contentType,
        long size,
        Instant createdAt,
        Long authorId,
        Instant removedAt
) {
}
