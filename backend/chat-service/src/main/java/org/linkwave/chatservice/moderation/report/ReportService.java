package org.linkwave.chatservice.moderation.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.users.AccessDto;
import org.linkwave.chatservice.api.users.UserServiceClient;
import org.linkwave.chatservice.api.ws.WSServiceClient;
import org.linkwave.chatservice.bot.BotService;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.common.BadRequestDataException;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.common.ResourceNotFoundException;
import org.linkwave.chatservice.message.Message;
import org.linkwave.chatservice.message.MessageRepository;
import org.linkwave.chatservice.message.text.TextMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    static final int MAX_REPORTS_PER_DAY = 20;
    static final String VIEW_PERMISSION = "moderation.view";
    static final String RESOLVE_PERMISSION = "moderation.resolve";

    private final MessageRepository messageRepository;
    private final MessageReportRepository reportRepository;
    private final ChatService chatService;
    private final UserServiceClient userServiceClient;
    private final BotService botService;
    private final WSServiceClient wsServiceClient;

    /** Mọi thành viên đoạn chat đều được báo cáo tin của người khác, kể cả khi người gửi là admin hoặc giảng viên. */
    public void report(RequestInitiator reporter, String messageId, ReportRequest request) {
        final Long reporterId = reporter.userId();
        final Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (reporterId.equals(message.getAuthorId())) {
            throw new BadRequestDataException("Không thể tự báo cáo tin nhắn của mình");
        }
        if (!chatService.isMember(reporterId, message.getChat())) {
            throw new PrivacyViolationException("Bạn không thuộc đoạn chat này");
        }
        if (reportRepository.existsByMessageIdAndReporterId(messageId, reporterId)) {
            throw new BadRequestDataException("Bạn đã báo cáo tin nhắn này");
        }
        final long today = reportRepository.countByReporterIdAndReportedAtAfter(
                reporterId, Instant.now().minus(1, ChronoUnit.DAYS));
        if (today >= MAX_REPORTS_PER_DAY) {
            throw new BadRequestDataException("Bạn đã đạt giới hạn báo cáo trong ngày");
        }

        final String text = message instanceof TextMessage textMessage ? textMessage.getText() : null;
        reportRepository.save(MessageReport.builder()
                .messageId(messageId)
                .chatId(message.getChat().getId())
                .authorId(message.getAuthorId())
                .reporterId(reporterId)
                .category(request.category())
                .reason(request.reason() == null || request.reason().isBlank() ? null : request.reason().trim())
                .messageText(text)
                .messageCreatedAt(message.getCreatedAt())
                .reportedAt(Instant.now())
                .status(ReportStatus.OPEN)
                .build());
        log.info("report: reporter={} message={} category={}", reporterId, messageId, request.category());
    }

    public List<MessageReport> listOpen(RequestInitiator viewer, int page, int size) {
        requirePermission(viewer, VIEW_PERMISSION);
        final Page<MessageReport> result = reportRepository.findByStatus(
                ReportStatus.OPEN, PageRequest.of(page, Math.min(size, 100), Sort.by("reportedAt")));
        log.info("moderation: viewer={} listed open reports page={}", viewer.userId(), page);
        return result.getContent();
    }

    public void resolve(RequestInitiator admin, String reportId, ResolveRequest request) {
        requirePermission(admin, RESOLVE_PERMISSION);
        final MessageReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BadRequestDataException("Báo cáo này đã được xử lý");
        }

        switch (request.decision()) {
            case HIDE -> {
                messageRepository.findById(report.getMessageId()).ifPresent(message -> {
                    message.setRemovedAt(Instant.now());
                    if (message instanceof TextMessage textMessage) {
                        textMessage.setText(null);
                    }
                    messageRepository.save(message);
                });
                notifyRemoved(admin, report);
                // Gỡ nội dung luôn kèm cảnh báo gửi tới tác giả
                botService.warnAuthor(report.getAuthorId(), admin.bearer(), warningText(report, request.note()));
            }
            case WARN -> botService.warnAuthor(report.getAuthorId(), admin.bearer(), warningText(report, request.note()));
            case DISMISS -> { }
        }

        report.setStatus(ReportStatus.RESOLVED);
        report.setDecision(request.decision());
        report.setResolutionNote(request.note());
        report.setResolvedBy(admin.userId());
        report.setResolvedAt(Instant.now());
        reportRepository.save(report);
        log.info("moderation: admin={} resolved report={} decision={}", admin.userId(), reportId, request.decision());
    }

    private void requirePermission(RequestInitiator who, String permission) {
        final AccessDto access = userServiceClient.getAccess(who.userId(), who.bearer());
        if (access == null || !access.schoolPermissions().contains(permission)) {
            throw new PrivacyViolationException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private String warningText(MessageReport report, String note) {
        final StringBuilder sb = new StringBuilder("Cảnh báo từ quản trị: tin nhắn của bạn lúc ")
                .append(formatTime(report.getMessageCreatedAt()))
                .append(" đã vi phạm quy định cộng đồng và được xử lý.");
        if (report.getMessageText() != null) {
            final String quoted = report.getMessageText().length() > 200
                    ? report.getMessageText().substring(0, 200) + "…"
                    : report.getMessageText();
            sb.append(" Nội dung: \"").append(quoted).append("\".");
        }
        if (note != null && !note.isBlank()) {
            sb.append(" Ghi chú: ").append(note.trim());
        }
        return sb.toString();
    }


    private void notifyRemoved(RequestInitiator admin, MessageReport report) {
        try {
            wsServiceClient.notifyMessageRemoved(admin.bearer(), report.getChatId(),
                    new WSServiceClient.MessageRemovedNotification(report.getMessageId(), report.getAuthorId()));
        } catch (RuntimeException e) {
            log.warn("cannot push removal of message {}: {}", report.getMessageId(), e.getMessage());
        }
    }


    private static final DateTimeFormatter WARNING_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static String formatTime(Instant instant) {
        return instant == null ? "không rõ thời điểm" : WARNING_TIME.format(instant);
    }
}