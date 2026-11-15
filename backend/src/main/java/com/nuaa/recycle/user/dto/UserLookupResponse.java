package com.nuaa.recycle.user.dto;

public record UserLookupResponse(
        Long userId,
        String userCode,
        String nickname,
        String phoneMasked,
        String avatarUrl,
        Integer pointsBalance,
        String status
) {
}
