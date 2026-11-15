package com.nuaa.recycle.delivery.dto;

import java.time.LocalDateTime;

public record RevokeResponse(
        Long recordId,
        String status,
        Integer refundPoints,
        Integer balanceAfter,
        LocalDateTime revokedAt,
        Integer deficit,
        String revokedBy
) {
}
