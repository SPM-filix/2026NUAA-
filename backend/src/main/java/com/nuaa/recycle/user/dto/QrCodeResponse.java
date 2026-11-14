package com.nuaa.recycle.user.dto;

import java.time.LocalDateTime;

public record QrCodeResponse(
        String userCode,
        String qrContent,
        int expiresIn,
        LocalDateTime expiresAt
) {
}
