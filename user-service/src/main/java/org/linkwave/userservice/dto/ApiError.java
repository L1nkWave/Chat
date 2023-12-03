package org.linkwave.userservice.dto;

import java.time.ZonedDateTime;

public record ApiError(
        String path,
        String error,
        int status,
        ZonedDateTime timestamp) {
}