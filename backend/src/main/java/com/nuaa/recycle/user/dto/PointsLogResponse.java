package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record PointsLogResponse(
        Long logId,
        String type,
        Integer change,
        Integer balanceAfter,
        String bizType,
        String bizNo,
        String remark,
        LocalDateTime createdAt
) {
}
