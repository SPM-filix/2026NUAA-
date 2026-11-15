package com.nuaa.recycle.auth.dto;

/**
 * API-AUTH-01 用户注册响应（成功即登录）。
 * 字段与 docs/api.md 一致；mustChangePassword 为约定外的补充字段，注册时恒为 false。
 */
public record RegisterResponse(
        Long userId,
        String userCode,
        String token,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword
) {
}
