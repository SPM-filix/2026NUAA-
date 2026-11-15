package com.nuaa.recycle.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
