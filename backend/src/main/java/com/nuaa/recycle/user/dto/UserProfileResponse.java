package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long userId,
        String userCode,
        String phone,
        String nickname,
        String avatarUrl,
        String address,
        Integer pointsBalance,
        String status,
        LocalDateTime createdAt
) {
}
