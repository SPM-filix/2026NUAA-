package com.nuaa.recycle.exchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequest(
        @NotBlank(message = "取消原因不能为空")
        @Size(min = 10, max = 200, message = "取消原因需为 10-200 字")
        String reason
) {
}
