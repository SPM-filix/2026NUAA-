package com.nuaa.recycle.exchange.dto;

public record CancelResponse(
        Long orderId,
        String status,
        Integer refundPoints,
        Integer restoredStock,
        Integer balanceAfter
) {
}
