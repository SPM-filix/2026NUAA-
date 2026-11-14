package com.nuaa.recycle.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "密码需为 8-20 位且包含字母和数字")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(min = 1, max = 20, message = "昵称长度需为 1-20 字符")
        String nickname,

        @Size(max = 100, message = "地址不能超过 100 字符")
        String address
) {
}
