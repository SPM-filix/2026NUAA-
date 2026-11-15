package com.nuaa.recycle.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_rule")
public class PointsRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Integer pointsPerKg;
    private LocalDateTime effectiveFrom;
    private Long createdBy;
    private LocalDateTime createdAt;
}
