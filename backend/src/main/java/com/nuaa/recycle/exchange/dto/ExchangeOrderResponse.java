package com.nuaa.recycle.exchange.dto;

import java.time.LocalDateTime;

public record ExchangeOrderResponse(
        Long orderId,
        String orderNo,
        Long userId,
        String userNickname,
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
        Integer balanceAfter,
        LocalDateTime createdAt
) {
}
