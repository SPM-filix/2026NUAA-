package com.nuaa.recycle.user.dto;

public record PointsSummaryResponse(
        int balance,
        long totalIncome,
        long totalExpense,
        long totalRevoked
) {
}
