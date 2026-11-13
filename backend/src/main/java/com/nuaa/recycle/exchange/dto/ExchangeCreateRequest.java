package com.nuaa.recycle.exchange.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExchangeCreateRequest(
        @NotNull(message = "商品 id 不能为空")
        Long productId,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 5, message = "单次最多兑换 5 件")
        Integer quantity,

        @NotBlank(message = "幂等键不能为空")
        @Size(max = 64, message = "幂等键不能超过 64 字符")
        String idempotencyKey
) {
}
