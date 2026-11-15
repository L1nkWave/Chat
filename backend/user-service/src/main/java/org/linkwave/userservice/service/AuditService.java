package org.linkwave.userservice.service;

import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.entity.AuditLogEntity;
import org.linkwave.userservice.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void record(Long actorId, String action, String targetType, String targetId, String detail, String ip) {
        repository.save(AuditLogEntity.builder()
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .ip(ip)
                .build());
    }

    public List<AuditLogEntity> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

}