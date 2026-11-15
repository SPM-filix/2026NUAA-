package com.nuaa.recycle.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户申诉（API-USER-09/10 使用）。管理端处理见模块层 AccountController。
 */
@Data
@TableName("appeal")
public class Appeal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String description;
    private String status;
    private String conclusion;
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
