package com.nuaa.recycle.user.dto;

import jakarta.validation.constraints.Size;

/**
 * API-USER-02 修改本人资料。
 * phone、pointsBalance、status、role 不可修改，即使请求体传入也会被忽略。
 */
public record UserUpdateRequest(
        @Size(min = 1, max = 20, message = "昵称长度需为 1-20 字符")
        String nickname,

        @Size(max = 255, message = "头像地址不能超过 255 字符")
        String avatarUrl,

        @Size(max = 100, message = "地址不能超过 100 字符")
        String address
) {
}
