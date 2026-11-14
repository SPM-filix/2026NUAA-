package com.nuaa.recycle.delivery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryRecordResponse(
        Long recordId,
        String recordNo,
        Long userId,
        String userNickname,
        String userPhoneMasked,
        Long recyclerId,
        String recyclerName,
        String siteName,
        Long categoryId,
        String categoryName,
        BigDecimal weight,
        Integer points,
        String status,
        String remark,
        String revokeReason,
        String revokedBy,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        boolean revocable
) {
}
