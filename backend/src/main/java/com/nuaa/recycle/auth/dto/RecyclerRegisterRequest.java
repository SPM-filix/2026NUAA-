package com.nuaa.recycle.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecyclerRegisterRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(min = 2, max = 20, message = "姓名长度需为 2-20 字符")
        String name,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "密码需为 8-20 位且包含字母和数字")
        String password,

        @NotBlank(message = "工号不能为空")
        @Size(min = 4, max = 20, message = "工号长度需为 4-20 字符")
        String employeeNo,

        Long siteId,

        @Size(max = 200, message = "申请说明不能超过 200 字")
        String reason
) {
}
