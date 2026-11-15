package org.linkwave.chatservice.api.users;

import java.util.Set;

/** Phản hồi của user-service GET /api/v1/users/{id}/access. */
public record AccessDto(Long userId, int level, Set<String> schoolPermissions) {
}