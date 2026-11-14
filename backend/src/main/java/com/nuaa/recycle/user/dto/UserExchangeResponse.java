package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record UserExchangeResponse(
        Long orderId,
        String orderNo,
        Long productId,
        String productName,
        String productCoverUrl,
        Integer quantity,
        Integer pointsCost,
        String status,
        String redeemCode,
        LocalDateTime redeemCodeExpireAt,
        String siteName,
        String verifierName,
        LocalDateTime verifiedAt,
        String cancelReason,
        LocalDateTime createdAt
) {
}
