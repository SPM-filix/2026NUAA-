package com.nuaa.recycle.dto;

import java.time.LocalDateTime;

/**
 * API-CAT-01 响应。契约字段是 categoryId，不能直接把实体 id 序列化出去。
 */
public record CategoryResponse(
        Long categoryId,
        String name,
        String code,
        String unit,
        String iconUrl,
        Integer sort,
        String status,
        Integer pointsPerKg,
        LocalDateTime ruleEffectiveFrom
) {
}
