package com.nuaa.recycle.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_record")
public class DeliveryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long userId;
    private Long recyclerId;
    private Long siteId;
    private Long categoryId;
    private Long ruleId;
    private BigDecimal weight;
    private Integer points;
    private String status;
    private String remark;
    private String revokeReason;
    private String revokedByType;
    private Long revokedById;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
}
