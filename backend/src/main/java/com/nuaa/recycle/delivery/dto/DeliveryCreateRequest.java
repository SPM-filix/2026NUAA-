package com.nuaa.recycle.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DeliveryCreateRequest(
        @NotNull(message = "用户 id 不能为空")
        Long userId,

        @NotNull(message = "类别 id 不能为空")
        Long categoryId,

        @NotNull(message = "重量不能为空")
        @DecimalMin(value = "0.01", message = "重量不能小于 0.01 kg")
        @DecimalMax(value = "100.00", message = "重量不能超过 100 kg")
        @Digits(integer = 3, fraction = 2, message = "重量最多两位小数")
        BigDecimal weight,

        @Size(max = 100, message = "备注不能超过 100 字")
        String remark
) {
}
