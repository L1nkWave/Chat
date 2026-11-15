package org.linkwave.chatservice.moderation.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface MessageReportRepository extends MongoRepository<MessageReport, String> {

    boolean existsByMessageIdAndReporterId(String messageId, Long reporterId);

    long countByReporterIdAndReportedAtAfter(Long reporterId, Instant after);

    Page<MessageReport> findByStatus(ReportStatus status, Pageable pageable);

}