package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exchange_order")
public class ExchangeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Integer pointsCost;
    private String status;
    private String redeemCode;
    private LocalDateTime redeemExpireAt;
    private Long verifiedBy;
    private Long verifiedSiteId;
    private LocalDateTime verifiedAt;
    private String cancelReason;
    private Long cancelledBy;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
