package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record AppealCreateResponse(
        Long appealId,
        String status,
        LocalDateTime createdAt
) {
}
