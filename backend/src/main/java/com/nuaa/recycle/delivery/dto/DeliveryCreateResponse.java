package com.nuaa.recycle.delivery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryCreateResponse(
        Long recordId,
        String recordNo,
        Integer points,
        BigDecimal weight,
        String categoryName,
        Integer balanceAfter,
        LocalDateTime createdAt,
        LocalDateTime revocableBefore
) {
}
