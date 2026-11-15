package com.nuaa.recycle.common;

import lombok.Data;

@Data
public class LoginUser {
    private Long userId;
    private String role;
    private Integer tokenVersion;
    private String token;
}
