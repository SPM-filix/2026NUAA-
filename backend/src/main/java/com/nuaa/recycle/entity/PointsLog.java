package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_log")
public class PointsLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private Integer change;
    private Integer balanceAfter;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
