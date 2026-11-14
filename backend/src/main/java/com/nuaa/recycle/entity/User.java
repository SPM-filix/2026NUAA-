package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userCode;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String address;
    private Integer pointsBalance;
    private Integer tokenVersion;
    private Boolean mustChangePassword;
    private String status;
    private String disabledReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
