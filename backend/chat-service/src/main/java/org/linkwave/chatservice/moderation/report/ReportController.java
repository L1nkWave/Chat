package org.linkwave.chatservice.moderation.report;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/chats/messages/{messageId}/report")
    @ResponseStatus(CREATED)
    public void report(@PathVariable String messageId,
                       @RequestBody @Valid ReportRequest body,
                       @NonNull HttpServletRequest request) {
        reportService.report(requestInitiator(request), messageId, body);
    }

    @GetMapping("/moderation/reports")
    public List<MessageReport> listOpen(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @NonNull HttpServletRequest request) {
        return reportService.listOpen(requestInitiator(request), page, size);
    }

    @PostMapping("/moderation/reports/{reportId}/resolve")
    @ResponseStatus(NO_CONTENT)
    public void resolve(@PathVariable String reportId,
                        @RequestBody @Valid ResolveRequest body,
                        @NonNull HttpServletRequest request) {
        reportService.resolve(requestInitiator(request), reportId, body);
    }

}