package com.nuaa.recycle.exchange.dto;

import java.time.LocalDateTime;

public record RedeemResponse(
        Long orderId,
        String orderNo,
        String productName,
        Integer quantity,
        String userNickname,
        String status,
        LocalDateTime verifiedAt,
        String verifierName
) {
}
