package com.nuaa.recycle.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeRequest(
        @NotBlank(message = "撤销原因不能为空")
        @Size(min = 10, max = 100, message = "撤销原因需为 10-100 字")
        String reason
) {
}
