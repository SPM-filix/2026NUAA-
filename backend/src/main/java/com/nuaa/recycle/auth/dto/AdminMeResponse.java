package com.nuaa.recycle.auth.dto;

import java.time.LocalDateTime;

public record AdminMeResponse(
        Long adminId,
        String username,
        LocalDateTime createdAt
) {
}
