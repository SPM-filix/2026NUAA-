package com.nuaa.recycle.exchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemRequest(
        @NotBlank(message = "核销码不能为空")
        @Size(max = 8, message = "核销码长度为 8 位")
        String redeemCode
) {
}
