package org.linkwave.chatservice.moderation.report;

import java.time.LocalDate;

/**
 * Bộ lọc báo cáo. Mọi trường đều tùy chọn (null hoặc chuỗi rỗng nghĩa là không lọc).
 * status: OPEN | RESOLVED | ALL. category: một trong các ReportCategory.
 * from, to: ngày theo giờ Việt Nam, gồm cả hai đầu. q: tìm trong nội dung tin đã báo cáo (không phân biệt hoa thường).
 */
public record ReportFilter(
        String status,
        String category,
        Long authorId,
        Long reporterId,
        LocalDate from,
        LocalDate to,
        String q
) {
}