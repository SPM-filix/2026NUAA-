package com.nuaa.recycle.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword,
        UserInfo user,
        RecyclerInfo recycler,
        AdminInfo admin
) {

    public record UserInfo(Long userId, String userCode, String nickname,
                           String avatarUrl, Integer pointsBalance) {
    }

    public record RecyclerInfo(Long recyclerId, String name, Long siteId, String siteName, String status) {
    }

    public record AdminInfo(Long adminId, String username) {
    }
}
