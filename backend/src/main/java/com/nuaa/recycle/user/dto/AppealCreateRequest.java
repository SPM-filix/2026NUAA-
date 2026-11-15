package com.nuaa.recycle.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AppealCreateRequest(
        @NotBlank(message = "申诉对象类型不能为空")
        @Pattern(regexp = "DELIVERY|EXCHANGE", message = "申诉对象类型只能为 DELIVERY 或 EXCHANGE")
        String targetType,

        @NotNull(message = "申诉对象 id 不能为空")
        Long targetId,

        @NotBlank(message = "问题描述不能为空")
        @Size(min = 10, max = 500, message = "问题描述需为 10-500 字")
        String description
) {
}
