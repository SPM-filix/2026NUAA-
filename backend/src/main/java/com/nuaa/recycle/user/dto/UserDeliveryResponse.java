package com.nuaa.recycle.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserDeliveryResponse(
        Long recordId,
        String recordNo,
        Long categoryId,
        String categoryName,
        BigDecimal weight,
        Integer points,
        String recyclerName,
        String siteName,
        String status,
        String revokeReason,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {
}
