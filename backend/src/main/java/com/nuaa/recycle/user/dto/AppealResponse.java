package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record AppealResponse(
        Long appealId,
        String targetType,
        Long targetId,
        String targetNo,
        String description,
        String status,
        String conclusion,
        LocalDateTime handledAt,
        LocalDateTime createdAt
) {
}
