package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recycle_category")
public class RecycleCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String unit;
    private String iconUrl;
    private Integer sort;
    private String status;
    @TableField(exist = false)
    private Integer pointsPerKg;
    @TableField(exist = false)
    private LocalDateTime ruleEffectiveFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
