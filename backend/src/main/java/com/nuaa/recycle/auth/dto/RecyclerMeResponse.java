package com.nuaa.recycle.auth.dto;

import java.time.LocalDateTime;

public record RecyclerMeResponse(
        Long recyclerId,
        String name,
        String phone,
        String employeeNo,
        Long siteId,
        String siteName,
        String status,
        LocalDateTime createdAt
) {
}
