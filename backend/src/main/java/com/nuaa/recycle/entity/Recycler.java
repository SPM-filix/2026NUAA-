package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recycler")
public class Recycler {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private String passwordHash;
    private String employeeNo;
    private Long siteId;
    private String status;
    private Integer tokenVersion;
    private Boolean mustChangePassword;
    private String applyReason;
    private String rejectReason;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
