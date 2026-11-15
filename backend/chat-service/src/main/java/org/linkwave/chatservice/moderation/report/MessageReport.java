package org.linkwave.chatservice.moderation.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Báo cáo tin nhắn. Nội dung tin được lưu kèm để có bằng chứng. Tự xóa sau 90 ngày. */
@Document("message_reports")
@CompoundIndex(name = "uniq_report_per_user", def = "{'messageId': 1, 'reporterId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReport {

    @Id
    private String id;

    private String messageId;
    private String chatId;
    private Long authorId;
    private Long reporterId;

    private ReportCategory category;
    private String reason;

    /** Nội dung tin tại thời điểm báo cáo (null nếu tin là file hoặc đã bị xóa trước đó). */
    private String messageText;
    private Instant messageCreatedAt;

    @Indexed(expireAfter = "P90D")
    private Instant reportedAt;

    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    private ResolveDecision decision;
    private String resolutionNote;
    private Long resolvedBy;
    private Instant resolvedAt;

}