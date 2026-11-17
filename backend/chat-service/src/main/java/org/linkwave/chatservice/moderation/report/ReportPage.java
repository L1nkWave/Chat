package org.linkwave.chatservice.moderation.report;

import java.util.List;

/** Một trang kết quả báo cáo, kèm tổng số bản ghi khớp bộ lọc. */
public record ReportPage(List<MessageReport> items, long total, int page, int size) {
}